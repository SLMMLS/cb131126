package com.clipsync.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.clipsync.app.data.PairedPc
import com.clipsync.app.data.Repository
import com.clipsync.app.service.ReceiverControl
import com.clipsync.app.service.ReceiverService
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * "The database hasn't answered yet" must not look like "no PC is paired".
 * The old code used `null` for both, so on every cold start the NavHost was
 * built with startDestination = "pairing" a few milliseconds before Room
 * delivered the real value - and a paired phone landed on the QR scanner
 * every launch. The shell is built only once the answer is known.
 */
private sealed interface PairedState {
    data object Loading : PairedState
    data class Known(val pc: PairedPc?) : PairedState
}

class MainActivity : ComponentActivity() {
    companion object {
        /** Set by the file notifications: which tab to open. */
        const val EXTRA_ROUTE = "route"
        const val ROUTE_FILES = "files"
    }

    private val repository by lazy { Repository(applicationContext) }

    /**
     * The phone's clipboard cannot be watched in the background (Android 10+), but it can be read
     * while this window has focus - so every time ClipSync comes to the front, whatever is on the
     * clipboard is added to the history. It is the only way to build a history of what was copied
     * on the phone, and it has to happen here, not in onResume: the focus check is on the window,
     * and the window only has focus after onResume.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && repository.autoCapture) {
            lifecycleScope.launch { repository.captureClipboard() }
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* fine either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is drawn edge to edge, black under transparent system bars (Android 15 makes this the rule anyway).
        // The "dark" style means light icons in the status bar and on the navigation bar.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            ClipSyncTheme {
                GlassBackground {
                    val pairedFlow = remember { repository.pairedPc.map<PairedPc?, PairedState> { PairedState.Known(it) } }
                    val state by pairedFlow.collectAsState(initial = PairedState.Loading)

                    when (val current = state) {
                        PairedState.Loading -> Unit // a black frame or two while Room answers
                        is PairedState.Known -> {
                            val pc = current.pc

                            // Keep the receiver alive whenever a PC is paired, not just
                            // while this screen happens to be open - and stop it once
                            // the PC is unpaired, so no listener outlives its purpose.
                            LaunchedEffect(pc?.deviceId) {
                                val serviceIntent = Intent(this@MainActivity, ReceiverService::class.java)
                                if (pc != null) {
                                    // The switch "Приём" can be off: then the receiver stays off until
                                    // the user flips it back (see ReceiverControl).
                                    if (ReceiverControl.shouldAutoStart(this@MainActivity)) {
                                        try {
                                            ContextCompat.startForegroundService(this@MainActivity, serviceIntent)
                                        } catch (e: Exception) {
                                            Log.w("ClipSync", "could not start ReceiverService", e)
                                        }
                                    }
                                } else {
                                    stopService(serviceIntent)
                                }
                            }

                            // Used once: turning the phone must not open the tab again after the person left it.
                            var openFiles by remember { mutableStateOf(intent?.getStringExtra(EXTRA_ROUTE) == ROUTE_FILES) }

                            AppShell(
                                repository = repository,
                                pc = pc,
                                openFiles = pc != null && openFiles,
                                onOpenFilesHandled = {
                                    openFiles = false
                                    intent?.removeExtra(EXTRA_ROUTE)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
