package com.clipsync.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.clipsync.app.data.PairedPc
import com.clipsync.app.data.Repository
import com.clipsync.app.files.FilesSettings
import com.clipsync.app.network.HelloSender
import com.clipsync.app.network.describeHello
import com.clipsync.app.service.ReceiverControl
import kotlinx.coroutines.launch

/**
 * The "ПК" tab: everything about the one paired computer in one place - who it is, whether the phone can reach it,
 * what it may send here - and, when there is none, how to pair one. Before this tab existed these controls were
 * spread over the settings screen and a full-screen scanner that took over the app.
 */
@Composable
fun PcScreen(repository: Repository, pc: PairedPc?, onPair: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var diagnosis by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var checkedAt by remember { mutableStateOf(0L) }
    var confirmUnpair by remember { mutableStateOf(false) }
    val receiverEnabled by ReceiverControl.enabled(context).collectAsState()
    var stopOnClose by remember { mutableStateOf(ReceiverControl.stopOnClose(context)) }
    var acceptFiles by remember { mutableStateOf(FilesSettings.acceptIncoming(context)) }

    ScreenScaffold(title = "ПК") {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (pc == null) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("ПК не подключён", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Чтобы буфер обмена и файлы ходили между телефоном и компьютером, подключите компьютер. Это нужно сделать один раз.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("1.  На компьютере: ClipSync → Настройки → Устройства → «Добавить устройство» — появится QR-код.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("2.  Здесь: «Сканировать QR» и наведите камеру на код.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Телефон и компьютер должны быть в одной сети Wi-Fi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    AccentButton("Сканировать QR", onClick = onPair, modifier = Modifier.fillMaxWidth())
                }
            } else {
                // What the phone knows about the link: the last "я в сети" of this session. It is re-read after "Проверить связь".
                val last = remember(checkedAt, pc.deviceId) { HelloSender.last }
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(
                            when (last?.ok) {
                                true -> Color(Palette.CYAN)
                                false -> Color(Palette.DANGER)
                                null -> Color(Palette.INK_FAINT)
                            },
                            size = 10.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(pc.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(pc.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (last == null) "Связь в этом сеансе ещё не проверялась." else describeHello(last, System.currentTimeMillis()),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (last?.ok == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassButton(
                            text = if (checking) "Проверяю…" else "Проверить связь",
                            enabled = !checking,
                            onClick = {
                                scope.launch {
                                    checking = true
                                    diagnosis = repository.diagnose()
                                    checking = false
                                    checkedAt = System.currentTimeMillis()
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                        DangerButton(text = "Отключить", onClick = { confirmUnpair = true })
                    }
                    diagnosis?.let { report ->
                        Spacer(Modifier.height(12.dp))
                        Text(report, style = MaterialTheme.typography.bodySmall)
                    }
                }

                SectionLabel("Приём с ПК")
                GlassCard(Modifier.fillMaxWidth()) {
                    SwitchRow(
                        title = "Принимать буфер с ПК",
                        description = if (receiverEnabled) {
                            "Включено: ClipSync слушает ПК в фоне, пока видно уведомление «ClipSync активен». " +
                                "Выключить можно здесь или кнопкой «Выключить» в этом уведомлении."
                        } else {
                            "Выключено: ПК знает об этом и ничего не отправляет. Отправка с телефона на ПК " +
                                "работает по-прежнему."
                        },
                        checked = receiverEnabled,
                        onCheckedChange = { if (it) ReceiverControl.turnOn(context) else ReceiverControl.turnOff(context) },
                    )
                    GlassDivider()
                    SwitchRow(
                        title = "Останавливать при закрытии приложения",
                        description = "Если включено, приём выключается, когда вы смахиваете ClipSync из списка недавних " +
                            "приложений, и снова включается при его открытии. Если выключено (по умолчанию), " +
                            "приём продолжает работать в фоне, чтобы ПК мог присылать записи.",
                        checked = stopOnClose,
                        onCheckedChange = {
                            stopOnClose = it
                            ReceiverControl.setStopOnClose(context, it)
                        },
                    )
                    GlassDivider()
                    SwitchRow(
                        title = "Принимать файлы с ПК",
                        description = "Файлы, которые вы отправляете с компьютера (перетаскиванием в окно ClipSync), " +
                            "сохраняются в «Загрузки/ClipSync». Выключите, если с ПК нужен только буфер обмена. " +
                            "Программы и установочные файлы ClipSync никогда не запускает сам.",
                        checked = acceptFiles,
                        onCheckedChange = {
                            acceptFiles = it
                            FilesSettings.setAcceptIncoming(context, it)
                        },
                    )
                }
            }
        }
    }

    if (confirmUnpair && pc != null) {
        AlertDialog(
            onDismissRequest = { confirmUnpair = false },
            containerColor = Color(Palette.SOLID_HIGH),
            title = { Text("Отключить ПК?") },
            text = {
                Text(
                    "Телефон перестанет получать записи с «${pc.name}» и отправлять их туда. " +
                        "Чтобы подключить компьютер снова, нужно будет заново отсканировать QR-код.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmUnpair = false
                    diagnosis = null
                    scope.launch { repository.unpair() }
                }) { Text("Отключить", color = Color(Palette.DANGER)) }
            },
            dismissButton = { TextButton(onClick = { confirmUnpair = false }) { Text("Отмена") } },
        )
    }
}
