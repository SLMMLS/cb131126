package com.clipsync.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.clipsync.app.data.PairedPc
import com.clipsync.app.data.Repository
import com.clipsync.app.files.TState
import com.clipsync.app.files.Transfers

/**
 * Moves to a tab the way tab bars are meant to work: the tab you leave keeps its history (a record you had open is
 * still open when you come back), tapping the tab you are on does nothing, and the back button goes to the first
 * tab and then leaves the app instead of walking through every tab you visited.
 */
internal fun NavController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Leaves [route] - but only if that is where the person still is. A late answer (the pairing that finished after the
 * person pressed "назад", a record deleted from elsewhere) must not pop whatever happens to be on screen now: popping
 * the last screen of the stack leaves a blank window.
 */
private fun NavController.leave(route: String) {
    if (currentDestination?.route == route) popBackStack()
}

/**
 * The frame of the app once the database has said whether a PC is paired: the four tabs and, on top of them, the
 * record and the QR scanner (without the bar).
 *
 * "No PC paired" is a state of the tabs, not a different app: you land on "ПК" with the button that starts the
 * pairing, "Буфер" still shows what is on the phone, "Файлы" says that a PC is needed first.
 */
@Composable
internal fun AppShell(repository: Repository, pc: PairedPc?, openFiles: Boolean, onOpenFilesHandled: () -> Unit) {
    val navController = rememberNavController()

    // Decided once. The graph's first screen must not change while the app runs: pairing and unpairing move the
    // person with explicit navigation below, not by swapping the graph's start from under the back stack.
    val startRoute = remember { if (pc == null) AppTab.PC.route else AppTab.BUFFER.route }

    // Screens read the PC through this, not through the argument they were built with, so that they follow
    // pairing and unpairing without the graph being rebuilt.
    val currentPc by rememberUpdatedState(pc)

    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val showBar = AppTab.values().any { it.route == route }

    val transfers by Transfers.registry.views.collectAsState()
    val running = remember(transfers) { transfers.count { it.state == TState.ACTIVE || it.state == TState.QUEUED } }

    // A tap on a file notification opens "Файлы" straight away.
    LaunchedEffect(openFiles) {
        if (openFiles) {
            navController.switchTab(AppTab.FILES.route)
            onOpenFilesHandled()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            if (showBar) GlassNavigationBar(selectedRoute = route, activeTransfers = running) { tab -> navController.switchTab(tab.route) }
        },
    ) { inner ->
        NavHost(
            navController,
            startDestination = startRoute,
            modifier = Modifier.padding(inner),
            // Material's default is a 700 ms fade, which is slow for moving between tabs.
            enterTransition = { fadeIn(tween(160)) },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(160)) },
            popExitTransition = { fadeOut(tween(120)) },
        ) {
            composable(AppTab.BUFFER.route) {
                HomeScreen(
                    repository = repository,
                    pc = currentPc,
                    onOpenEntry = { entryId -> navController.navigate(Routes.entry(entryId)) },
                    onGoToPc = { navController.switchTab(AppTab.PC.route) },
                )
            }
            composable(AppTab.FILES.route) {
                FilesScreen(pc = currentPc, onGoToPc = { navController.switchTab(AppTab.PC.route) })
            }
            composable(AppTab.PC.route) {
                PcScreen(repository = repository, pc = currentPc, onPair = { navController.navigate(Routes.PAIRING) })
            }
            composable(AppTab.SETTINGS.route) {
                SettingsScreen(repository = repository)
            }
            composable(Routes.ENTRY) { navEntry ->
                EntryScreen(
                    repository = repository,
                    id = navEntry.arguments?.getString("id").orEmpty(),
                    onBack = { navController.leave(Routes.ENTRY) },
                )
            }
            composable(Routes.PAIRING) {
                PairingScreen(
                    repository = repository,
                    // Back to "ПК", which now shows the PC that was just paired.
                    onPaired = { navController.leave(Routes.PAIRING) },
                    onBack = { navController.leave(Routes.PAIRING) },
                )
            }
        }
    }
}
