package com.clipsync.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clipsync.app.data.PairedPc
import com.clipsync.app.files.Direction
import com.clipsync.app.files.FilesSettings
import com.clipsync.app.files.PublicDownloads
import com.clipsync.app.files.TState
import com.clipsync.app.files.TransferView
import com.clipsync.app.files.Transfers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * The "Файлы" tab: send files to the PC, and see what was sent and received. Files that come from the PC arrive by
 * themselves (the PC's window has a drop zone) and are listed here too; the list is the same one the notifications
 * and the sending queue use (`Transfers`).
 *
 * A tap opens a received file; a long press (or the dots) offers what else can be done with it.
 */
@Composable
fun FilesScreen(pc: PairedPc?, onGoToPc: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val transfers by Transfers.registry.views.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var preparing by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf<TransferView?>(null) }
    val acceptIncoming = remember { FilesSettings.acceptIncoming(context) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            preparing = true
            val prepared = withContext(Dispatchers.IO) { Transfers.prepare(context, uris, stage = false) }
            preparing = false
            if (prepared.sources.isNotEmpty()) Transfers.manager.enqueue(prepared.sources)
            if (prepared.problems.isNotEmpty()) snackbar.showSnackbar("Пропущено: " + prepared.problems.joinToString("; "))
        }
    }

    ScreenScaffold(
        title = "Файлы",
        snackbarHost = snackbar,
        actions = {
            if (transfers.any { it.state != TState.ACTIVE && it.state != TState.QUEUED }) {
                TextButton(onClick = { Transfers.manager.clear(null) }) { Text("Очистить") }
            }
        },
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)) {
            if (pc != null) {
                AccentButton(
                    text = if (preparing) "Подготовка…" else "Отправить файлы на ПК",
                    onClick = { picker.launch(arrayOf("*/*")) },
                    enabled = !preparing,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Файлы идут напрямую по Wi-Fi между вашими устройствами, в зашифрованном виде. " +
                        "С компьютера их можно отправить, перетащив в окно ClipSync: они появятся в «${PublicDownloads.whereToLook()}». " +
                        "Отправить можно и из любого приложения через «Поделиться» → «Файл на ПК».",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!acceptIncoming) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Приём файлов с ПК выключен (вкладка «ПК» → «Принимать файлы с ПК»).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("Сначала подключите ПК", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Файлы передаются между телефоном и сопряжённым компьютером. Подключение занимает полминуты: нужно отсканировать QR-код.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    GlassButton("Подключить ПК", onClick = onGoToPc)
                }
            }
        }

        if (transfers.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.TopCenter) {
                Text(
                    "Здесь появятся отправленные и полученные файлы.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            ) {
                items(transfers, key = { it.id }) { view ->
                    TransferRow(
                        view = view,
                        onMessage = { message -> scope.launch { snackbar.showSnackbar(message) } },
                        onAskDelete = { askDelete = it },
                    )
                }
            }
        }
    }

    askDelete?.let { view ->
        AlertDialog(
            onDismissRequest = { askDelete = null },
            containerColor = Color(Palette.SOLID_HIGH),
            title = { Text("Удалить файл?") },
            text = { Text("«${view.name}» будет удалён с телефона. Копия на компьютере останется.") },
            confirmButton = {
                TextButton(onClick = {
                    askDelete = null
                    when (val result = deleteReceived(context, view)) {
                        DeleteResult.Deleted -> {
                            Transfers.manager.clear(view.id)
                            scope.launch { snackbar.showSnackbar("Файл удалён") }
                        }
                        DeleteResult.AlreadyGone -> {
                            Transfers.manager.clear(view.id)
                            scope.launch { snackbar.showSnackbar("Файла уже не было на месте: запись убрана") }
                        }
                        is DeleteResult.Failed -> scope.launch { snackbar.showSnackbar(result.sentence) }
                    }
                }) { Text("Удалить", color = Color(Palette.DANGER)) }
            },
            dismissButton = { TextButton(onClick = { askDelete = null }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun TransferRow(view: TransferView, onMessage: (String) -> Unit, onAskDelete: (TransferView) -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val running = view.state == TState.ACTIVE || view.state == TState.QUEUED
    val incoming = view.direction == Direction.IN
    val received = incoming && view.state == TState.COMPLETE && view.location != null
    var menuOpen by remember { mutableStateOf(false) }
    val accent = if (incoming) Color(Palette.CYAN) else Color(Palette.AMBER)

    Box {
        GlassCard(
            Modifier.fillMaxWidth(),
            // A finished received file opens on a tap. Everything else has nothing to open, so a tap does nothing
            // (and a failed row keeps its own buttons).
            onClick = if (received) {
                { openReceived(context, view)?.let(onMessage) }
            } else {
                null
            },
            onLongClick = if (running) {
                null
            } else {
                {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuOpen = true
                }
            },
            contentPadding = PaddingValues(start = 12.dp, top = 12.dp, bottom = 8.dp, end = 4.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                FileTile(view.name, thumbnailSource(view))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(view.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (incoming) "↓" else "↑", color = accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            metaLine(view),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (view.state == TState.ACTIVE) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { fraction(view) },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)),
                            color = accent,
                            trackColor = Color(Palette.GLASS_EDGE_BOTTOM),
                        )
                    }
                    val note = view.error
                    if (!note.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            note,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (view.state == TState.FAILED || view.state == TState.CANCELLED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (running || view.canRetry) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            if (running) {
                                TextButton(onClick = { cancel(view) }) { Text("Отменить") }
                            } else {
                                TextButton(onClick = { Transfers.manager.retry(view.id)?.let(onMessage) }) { Text("Повторить") }
                            }
                        }
                    }
                }
                if (!running) {
                    // The same menu as a long press, for those who do not know about long presses.
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Действия", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            if (received) {
                DropdownMenuItem(
                    text = { Text("Открыть") },
                    onClick = {
                        menuOpen = false
                        openReceived(context, view)?.let(onMessage)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Поделиться") },
                    onClick = {
                        menuOpen = false
                        shareReceived(context, view)?.let(onMessage)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Показать в Загрузках") },
                    onClick = {
                        menuOpen = false
                        openDownloads(context)?.let(onMessage)
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("Убрать из списка") },
                onClick = {
                    menuOpen = false
                    Transfers.manager.clear(view.id)
                },
            )
            if (received) {
                DropdownMenuItem(
                    text = { Text("Удалить файл", color = Color(Palette.DANGER)) },
                    onClick = {
                        menuOpen = false
                        onAskDelete(view)
                    },
                )
            }
        }
    }
}

/** "Отменить": an outgoing transfer is stopped by the queue, an incoming one by the receiver. */
private fun cancel(view: TransferView) {
    if (view.direction == Direction.OUT) Transfers.manager.cancel(view.id) else Transfers.receiver.cancelIncoming(view.id)
}

private fun fraction(view: TransferView): Float =
    if (view.size <= 0) 0f else (view.transferred.toDouble() / view.size).toFloat().coerceIn(0f, 1f)

private fun metaLine(view: TransferView): String {
    val who = if (view.direction == Direction.IN) "с ПК" else "на ПК"
    val size = formatBytes(view.size)
    return when (view.state) {
        TState.QUEUED -> "$size · $who · в очереди"
        TState.ACTIVE -> buildString {
            append("${formatBytes(view.transferred)} из $size · ${(fraction(view) * 100).toInt()}%")
            if (view.rateBps > 0) {
                append(" · ${formatBytes(view.rateBps)}/с")
                val left = ((view.size - view.transferred) / view.rateBps).coerceAtLeast(0)
                append(" · осталось ${formatDuration(left)}")
            }
            append(" · $who")
        }
        TState.COMPLETE -> "$size · $who · ${view.finishedAt?.let { DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it)) } ?: ""}".trimEnd(' ', '·')
        TState.CANCELLED -> "$size · $who · отменено"
        TState.FAILED -> "$size · $who · не удалось"
    }
}

/** 1536 -> "1,5 КБ"; the same style as the PC window. */
internal fun formatBytes(bytes: Long): String {
    if (bytes < 0) return ""
    val units = listOf("Б", "КБ", "МБ", "ГБ")
    var value = bytes.toDouble()
    var i = 0
    while (value >= 1024 && i < units.size - 1) {
        value /= 1024
        i += 1
    }
    val digits = if (i == 0) 0 else if (value < 10) 1 else 0
    return String.format(Locale("ru"), "%.${digits}f %s", value, units[i])
}

private fun formatDuration(seconds: Long): String = when {
    seconds < 60 -> "${maxOf(1, seconds)} с"
    seconds < 3600 -> "${seconds / 60} мин ${seconds % 60} с"
    else -> "${seconds / 3600} ч ${(seconds % 3600) / 60} мин"
}
