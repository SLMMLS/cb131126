package com.clipsync.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clipsync.app.data.CaptureResult
import com.clipsync.app.data.EntryMeta
import com.clipsync.app.data.PairedPc
import com.clipsync.app.data.Repository
import com.clipsync.app.network.NetworkErrors
import com.clipsync.app.service.ReceiverControl
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val FILTER_TITLES = listOf("Все", "Телефон", "С ПК")

private val timeFormat = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())

/** "Where it came from · when · pinned" - shared by the list rows and the preview screen. */
internal fun entrySubtitle(entry: EntryMeta): String {
    val origin = when (entry.direction) {
        "incoming" -> "С ПК"
        "outgoing" -> "Отправлено на ПК"
        else -> "Буфер телефона"
    }
    return buildString {
        append(origin)
        append(" · ")
        append(timeFormat.format(Date(entry.createdAt)))
        if (entry.pinned) append(" · закреплено")
    }
}

/** The dot before the sub-line: cyan came from the PC, amber went to it, grey is the phone's own. */
internal fun entryOriginColor(entry: EntryMeta): Color = when (entry.direction) {
    "incoming" -> Color(Palette.CYAN)
    "outgoing" -> Color(Palette.AMBER)
    else -> Color(Palette.INK_FAINT)
}

private fun emptyText(filter: Int): String = when (filter) {
    1 -> "Здесь появится то, что вы скопировали на телефоне или отправили на ПК.\n\n" +
        "Скопируйте что-нибудь и откройте ClipSync: запись добавится сама. " +
        "Выделенный в любом приложении текст можно сразу отправить пунктом «Отправить на ПК» в его меню."
    2 -> "С ПК пока ничего не пришло."
    else -> "История пуста.\n\nСкопируйте что-нибудь на телефоне или на ПК."
}

/** The "Буфер" tab: what was copied here and what came from the PC, and the button that sends the clipboard. */
@Composable
fun HomeScreen(repository: Repository, pc: PairedPc?, onOpenEntry: (String) -> Unit, onGoToPc: () -> Unit) {
    // One flow for the life of the screen: asking the repository again on every recomposition would restart the query each time.
    val entries by remember(repository) { repository.observeEntries() }.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    // Follows the switch whoever flips it - here, on the PC tab, or the "Выключить" button of the notification.
    val receiverOn by ReceiverControl.enabled(context).collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var filter by remember { mutableStateOf(0) }
    var sendingId by remember { mutableStateOf<String?>(null) }
    var sendingCurrent by remember { mutableStateOf(false) }

    val visible = remember(entries, filter) {
        when (filter) {
            1 -> entries.filter { it.direction != "incoming" } // what originated on this phone
            2 -> entries.filter { it.direction == "incoming" }
            else -> entries
        }
    }

    ScreenScaffold(
        title = "Буфер",
        snackbarHost = snackbar,
        actions = {
            if (pc != null) {
                ReceiverPill(on = receiverOn, onToggle = { if (it) ReceiverControl.turnOn(context) else ReceiverControl.turnOff(context) })
            }
        },
        hasFloatingActionButton = pc != null,
        floatingActionButton = {
            if (pc != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        // "Send what is on the clipboard right now": read it into the history first (so it
                        // is not lost if the PC is unreachable), then send that very entry.
                        if (!sendingCurrent) {
                            scope.launch {
                                sendingCurrent = true
                                val message = try {
                                    when (val captured = repository.captureClipboard()) {
                                        is CaptureResult.NoClip -> "Буфер обмена пуст"
                                        is CaptureResult.Added -> {
                                            repository.sendEntry(captured.id)
                                            "Отправлено на ПК"
                                        }
                                        is CaptureResult.Existing -> {
                                            repository.sendEntry(captured.id)
                                            "Отправлено на ПК"
                                        }
                                    }
                                } catch (e: Exception) {
                                    "Не отправлено: ${NetworkErrors.explain(e)}"
                                }
                                sendingCurrent = false
                                snackbar.showSnackbar(message)
                            }
                        }
                    },
                    containerColor = Color(Palette.AMBER),
                    contentColor = Color(Palette.ON_ACCENT),
                    icon = { Icon(Icons.Filled.Send, contentDescription = null) },
                    text = { Text(if (sendingCurrent) "Отправка…" else "Отправить буфер") },
                )
            }
        },
    ) {
        if (pc == null) {
            GlassCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), onClick = onGoToPc) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ПК не подключён", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "История телефона ведётся, но отправлять и принимать можно только после подключения.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Подключить", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        PillRow(FILTER_TITLES, selected = filter, onSelect = { filter = it }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        if (visible.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    emptyText(filter),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // The bottom space keeps the last row clear of the floating button.
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(visible, key = { it.id }) { entry ->
                    EntryRow(
                        repository = repository,
                        entry = entry,
                        canSend = pc != null,
                        sending = sendingId == entry.id,
                        onOpen = { onOpenEntry(entry.id) },
                        onCopy = {
                            scope.launch {
                                val text = repository.loadText(entry.id)
                                if (text == null) {
                                    snackbar.showSnackbar("Не удалось прочитать запись")
                                } else {
                                    clipboardManager.setText(AnnotatedString(text))
                                    snackbar.showSnackbar("Скопировано")
                                }
                            }
                        },
                        onSend = {
                            scope.launch {
                                sendingId = entry.id
                                val failure = try {
                                    repository.sendEntry(entry.id)
                                    null
                                } catch (e: Exception) {
                                    e
                                }
                                sendingId = null
                                snackbar.showSnackbar(
                                    if (failure == null) "Отправлено на ПК" else "Не отправлено: ${NetworkErrors.explain(failure)}",
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

/** "Приём" and its switch in one glass pill: the one control people reach for every day, so it stays on the first tab. */
@Composable
private fun ReceiverPill(on: Boolean, onToggle: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(Color(Palette.GLASS_TOP))
            .border(1.dp, Color(Palette.GLASS_EDGE_TOP), shape)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(if (on) Color(Palette.CYAN) else Color(Palette.INK_FAINT))
        Spacer(Modifier.width(8.dp))
        Text(if (on) "Приём" else "Приём выкл.", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.width(6.dp))
        Switch(checked = on, onCheckedChange = onToggle)
    }
}

@Composable
private fun EntryRow(
    repository: Repository,
    entry: EntryMeta,
    canSend: Boolean,
    sending: Boolean,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onSend: () -> Unit,
) {
    val isImage = entry.entryType == "image"
    val preview by produceState(initialValue = "", key1 = entry.id) {
        value = if (isImage) "" else repository.loadPreview(entry.id)
    }

    GlassCard(onClick = onOpen, contentPadding = PaddingValues(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isImage) {
                Thumbnail(repository, entry.id)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    if (isImage) "Изображение" else preview.ifEmpty { "(пусто)" },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(entryOriginColor(entry), size = 7.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        entrySubtitle(entry),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row {
                if (!isImage) {
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Копировать", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                // Only what originated on this phone, and only with a PC to send it to: sending back what the PC just sent makes no sense.
                if (canSend && entry.direction != "incoming") {
                    IconButton(onClick = onSend, enabled = !sending) {
                        if (sending) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Send, contentDescription = "Отправить на ПК", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

/** Square thumbnail of an image entry. Decoded off the main thread, once per entry (see Repository.loadThumbnail). */
@Composable
private fun Thumbnail(repository: Repository, id: String) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = id) {
        value = repository.loadThumbnail(id)?.asImageBitmap()
    }
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(Palette.GLASS_TOP)),
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = "Изображение",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
