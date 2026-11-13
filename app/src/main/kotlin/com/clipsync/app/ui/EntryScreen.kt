package com.clipsync.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.clipsync.app.data.Repository
import com.clipsync.app.network.NetworkErrors
import kotlinx.coroutines.launch

/**
 * Full view of one history entry: the whole text (selectable) or the whole image (pinch to zoom,
 * double-tap to toggle), with the actions that apply to it. The list rows only ever show a
 * preview; this is where "tap an item to see it properly" lands.
 */
@Composable
fun EntryScreen(repository: Repository, id: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val meta by remember(id) { repository.observeEntry(id) }.collectAsState(initial = null)

    var text by remember { mutableStateOf<String?>(null) }
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var seen by remember { mutableStateOf(false) }

    val current = meta
    val entryType = current?.entryType

    // The flow starts as null ("not loaded yet"); a null *after* the entry has been seen means it
    // was deleted - here, or by the history trimming - so leave instead of showing an empty screen.
    LaunchedEffect(current) {
        if (current != null) {
            seen = true
        } else if (seen) {
            onBack()
        }
    }

    LaunchedEffect(id, entryType) {
        when (entryType) {
            "image" -> {
                val loaded = repository.loadBitmap(id)
                if (loaded == null) loadFailed = true else bitmap = loaded.asImageBitmap()
            }
            "text" -> {
                val loaded = repository.loadText(id)
                if (loaded == null) loadFailed = true else text = loaded
            }
        }
    }

    ScreenScaffold(
        title = if (entryType == "image") "Изображение" else "Текст",
        onBack = onBack,
        snackbarHost = snackbar,
    ) {
        val shownBitmap = bitmap
        val shownText = text
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                loadFailed -> Text(
                    "Не удалось прочитать запись",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                )
                entryType == "image" && shownBitmap != null -> Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(Palette.GLASS_TOP)),
                ) { ZoomableImage(shownBitmap) }
                entryType == "text" && shownText != null -> GlassCard(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 4.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    SelectionContainer {
                        Text(
                            shownText,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                        )
                    }
                }
                else -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        if (current != null) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusDot(entryOriginColor(current), size = 7.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    entrySubtitle(current),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (current != null && current.direction != "incoming") {
                    AccentButton(
                        text = if (sending) "Отправка…" else "Отправить на ПК",
                        onClick = {
                            scope.launch {
                                sending = true
                                val failure = try {
                                    repository.sendEntry(id)
                                    null
                                } catch (e: Exception) {
                                    e
                                }
                                sending = false
                                snackbar.showSnackbar(
                                    if (failure == null) "Отправлено на ПК" else "Не отправлено: ${NetworkErrors.explain(failure)}",
                                )
                            }
                        },
                        enabled = !sending,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (entryType == "text" && shownText != null) {
                    GlassButton(
                        text = "Копировать",
                        onClick = {
                            clipboardManager.setText(AnnotatedString(shownText))
                            scope.launch { snackbar.showSnackbar("Скопировано") }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = { scope.launch { repository.togglePin(id, !(current?.pinned ?: false)) } },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (current?.pinned == true) "Открепить" else "Закрепить")
                }
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f)) {
                    Text("Удалить", color = Color(Palette.DANGER))
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Color(Palette.SOLID_HIGH),
            title = { Text("Удалить запись?") },
            text = { Text("Она исчезнет из истории на телефоне. Копия на ПК останется.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        scope.launch { repository.delete(id) } // the flow turns null -> the effect above leaves the screen
                    },
                ) {
                    Text("Удалить", color = Color(Palette.DANGER))
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
        )
    }
}

/** Pinch to zoom, drag to pan, double-tap to toggle between "fit" and 2.5x. */
@Composable
private fun ZoomableImage(bitmap: ImageBitmap) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Image(
        bitmap = bitmap,
        contentDescription = "Изображение",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 6f)
                    offset = if (scale <= 1f) Offset.Zero else offset + pan
                }
            }
            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
    )
}
