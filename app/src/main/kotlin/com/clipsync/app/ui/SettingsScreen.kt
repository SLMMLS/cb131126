package com.clipsync.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.clipsync.app.data.Repository
import com.clipsync.app.network.ProtocolInfo
import kotlinx.coroutines.launch

/**
 * The "Настройки" tab: what belongs to the phone itself. Everything about the paired computer (and what it may
 * send here) is on the "ПК" tab.
 */
@Composable
fun SettingsScreen(repository: Repository) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var autoCapture by remember { mutableStateOf(repository.autoCapture) }
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    ScreenScaffold(title = "Настройки", snackbarHost = snackbar) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel("История буфера телефона")
            GlassCard(Modifier.fillMaxWidth()) {
                SwitchRow(
                    title = "Запоминать буфер при открытии",
                    description = "Android не даёт приложениям следить за буфером в фоне (только то, которое сейчас " +
                        "на экране), поэтому ClipSync добавляет в историю то, что скопировано на телефоне, " +
                        "в момент, когда вы его открываете. На Android 12+ система при этом может показать " +
                        "«ClipSync вставил из буфера обмена». Выделенный текст можно отправить сразу, без " +
                        "копирования: «Отправить на ПК» в меню выделения.",
                    checked = autoCapture,
                    onCheckedChange = {
                        autoCapture = it
                        repository.autoCapture = it
                    },
                )
                Spacer(Modifier.height(14.dp))
                GlassButton(
                    text = "Очистить неотмеченную историю",
                    onClick = {
                        scope.launch {
                            repository.clearUnpinned()
                            snackbar.showSnackbar("Готово: остались только закреплённые записи")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionLabel("О приложении")
            GlassCard(Modifier.fillMaxWidth()) {
                Text(
                    if (version.isEmpty()) "ClipSync" else "ClipSync $version",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Протокол ${ProtocolInfo.VERSION}. Данные идут напрямую между вашими устройствами по локальной сети, " +
                        "в зашифрованном виде; сервера в интернете нет.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
