package com.clipsync.app.ui

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.OperationCanceledException
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clipsync.app.files.FileCategory
import com.clipsync.app.files.FileTypes
import com.clipsync.app.files.ThumbMath
import com.clipsync.app.files.TransferView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * Small pictures of files for the "Файлы" list (ROADMAP 4.2). A photo or a video is asked of the system at the size of the
 * tile - never the whole file is decoded - and kept in a cache in memory; anything else, or anything the system cannot
 * draw, gets a tile with its extension on it instead.
 *
 * Nothing here is ever sent anywhere, and the address is only *read*: the person's own files, the way a gallery reads them.
 */
internal object Thumbnails {
    private val cache = object : LruCache<String, Bitmap>(ThumbMath.cacheBytes(Runtime.getRuntime().maxMemory())) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    /** Addresses that gave nothing: a row scrolled out of view and back must not make the system try (and fail) again. */
    private val failed = HashSet<String>()

    /** A list of forty photos must not start forty decoders at once. */
    private val gate = Semaphore(2)

    /** What is already in memory, for the first frame of a tile. */
    fun cached(source: String?, px: Int): Bitmap? = if (source == null) null else cache.get(ThumbMath.key(source, px))

    suspend fun load(context: Context, source: String, category: FileCategory, px: Int): Bitmap? {
        val key = ThumbMath.key(source, px)
        cache.get(key)?.let { return it }
        if (synchronized(failed) { key in failed }) return null

        val app = context.applicationContext
        val signal = CancellationSignal()
        val bitmap = gate.withPermit {
            withContext(Dispatchers.IO) {
                // The tile left the screen: stop the decoder instead of finishing a picture nobody will see.
                val handle = coroutineContext.job.invokeOnCompletion { signal.cancel() }
                try {
                    create(app, Uri.parse(source), category, px, signal)
                } finally {
                    handle.dispose()
                }
            }
        }
        if (bitmap != null) {
            cache.put(key, bitmap)
        } else if (!signal.isCanceled) {
            synchronized(failed) { failed.add(key) }
        }
        return bitmap
    }

    private fun create(context: Context, uri: Uri, category: FileCategory, px: Int, signal: CancellationSignal): Bitmap? {
        // Android 10+: the system keeps thumbnails of its own (the ones the gallery shows) and makes them for any
        // provider that supports it, orientation and all.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri.scheme == ContentResolver.SCHEME_CONTENT) {
            try {
                return context.contentResolver.loadThumbnail(uri, Size(px, px), signal)
            } catch (_: OperationCanceledException) {
                return null
            } catch (_: Exception) {
                // Not every provider can; fall through to doing it ourselves.
            }
        }
        if (signal.isCanceled) return null
        return when (category) {
            FileCategory.IMAGE -> decodeImage(context, uri, px)
            FileCategory.VIDEO -> videoFrame(context, uri, px)
            else -> null
        }
    }

    private fun decodeImage(context: Context, uri: Uri, px: Int): Bitmap? = try {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            null
        } else {
            val options = BitmapFactory.Options().apply { inSampleSize = ThumbMath.sampleSize(bounds.outWidth, bounds.outHeight, px) }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            if (decoded == null) {
                null
            } else {
                val orientation = try {
                    resolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) } ?: 1
                } catch (_: Exception) {
                    1
                }
                val turn = ThumbMath.turnFor(orientation)
                if (turn.isIdentity) {
                    decoded
                } else {
                    val matrix = Matrix().apply {
                        postRotate(turn.degrees.toFloat())
                        if (turn.mirror) postScale(-1f, 1f)
                    }
                    Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                }
            }
        }
    } catch (_: Exception) {
        null
    } catch (_: OutOfMemoryError) {
        null
    }

    private fun videoFrame(context: Context, uri: Uri, px: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            // Scaled by the decoder: the frame is never held at full size.
            retriever.getScaledFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, px, px)
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}

/** Where to draw a thumbnail of this row from: the file that was picked (outgoing), or the received copy (incoming); null when there is none. */
internal fun thumbnailSource(view: TransferView): String? {
    view.preview?.let { return it }
    val location = view.location ?: return null
    return when {
        location.startsWith("content://") -> location
        // Android 9 keeps received files in the app's own folder: a plain path. fromFile escapes what a URI cannot hold ("#", "%", "?").
        location.startsWith("/") -> Uri.fromFile(File(location)).toString()
        else -> null
    }
}

/**
 * The square in front of a file's name: its thumbnail when it is a photo or a video the system could draw, otherwise
 * a tile tinted by the kind of file with the extension on it ("PDF", "ZIP").
 */
@Composable
internal fun FileTile(name: String, source: String?, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    val context = LocalContext.current
    val category = remember(name) { FileTypes.categoryOf(name) }
    val px = with(LocalDensity.current) { size.roundToPx() }
    val first = remember(source, px) { Thumbnails.cached(source, px)?.asImageBitmap() }
    val bitmap by produceState<ImageBitmap?>(initialValue = first, key1 = source, key2 = px) {
        if (value == null && source != null && category.hasThumbnail) {
            value = Thumbnails.load(context, source, category, px)?.asImageBitmap()
        }
    }
    val tint = Color(Palette.tileTint(category))
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(tint.copy(alpha = Palette.TILE_FILL_ALPHA))
            .border(1.dp, tint.copy(alpha = 0.35f), shape),
        contentAlignment = Alignment.Center,
    ) {
        val picture = bitmap
        if (picture != null) {
            Image(bitmap = picture, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            if (category == FileCategory.VIDEO) PlayBadge(Modifier.align(Alignment.BottomEnd))
        } else {
            Text(
                FileTypes.tileLabel(name),
                color = tint,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

/** A small "play" mark on a video's thumbnail. Drawn, not typed: the "▶" character turns into a coloured emoji on some phones. */
@Composable
private fun PlayBadge(modifier: Modifier = Modifier) {
    Canvas(modifier.size(22.dp)) {
        drawCircle(Color(0x99000000))
        val triangle = Path().apply {
            moveTo(size.width * 0.40f, size.height * 0.30f)
            lineTo(size.width * 0.40f, size.height * 0.70f)
            lineTo(size.width * 0.72f, size.height * 0.50f)
            close()
        }
        drawPath(triangle, Color(Palette.INK))
    }
}
