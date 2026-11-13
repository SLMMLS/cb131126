package com.clipsync.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * The four places of the app (ROADMAP 4.1). Every tab keeps its own history: from "Буфер" you can open a record,
 * move to "Файлы" and come back to the record. The records themselves and the QR scanner are *not* tabs: they open
 * on top, without the bar, and "назад" closes them.
 */
internal enum class AppTab(val route: String, val label: String, val icon: ImageVector) {
    BUFFER("buffer", "Буфер", Icons.Filled.ContentPaste),
    FILES("files", "Файлы", Icons.Filled.Folder),
    PC("pc", "ПК", Icons.Filled.Computer),
    SETTINGS("settings", "Настройки", Icons.Filled.Settings),
}

internal object Routes {
    const val ENTRY = "entry/{id}"
    const val PAIRING = "pairing"

    fun entry(id: String) = "entry/$id"
}

/**
 * The bar: the PC window's glass colour, nearly opaque, with a hairline on top; the selected tab is an amber pill
 * with dark text on it (the same amber as the main buttons), the others are quiet. A small cyan badge on "Файлы"
 * says how many transfers are running or waiting, so that you can see from any tab that something is going on.
 */
@Composable
internal fun GlassNavigationBar(selectedRoute: String?, activeTransfers: Int, onSelect: (AppTab) -> Unit) {
    val edge = Color(Palette.GLASS_EDGE_TOP)
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color(Palette.BAR))
            .drawBehind { drawLine(edge, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx()) },
    ) {
        NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
            for (tab in AppTab.values()) {
                NavigationBarItem(
                    selected = tab.route == selectedRoute,
                    onClick = { onSelect(tab) },
                    icon = {
                        if (tab == AppTab.FILES && activeTransfers > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = Color(Palette.CYAN), contentColor = Color(Palette.ON_ACCENT)) {
                                        Text(if (activeTransfers > 9) "9+" else activeTransfers.toString())
                                    }
                                },
                            ) { Icon(tab.icon, contentDescription = null) }
                        } else {
                            Icon(tab.icon, contentDescription = null)
                        }
                    },
                    label = { Text(tab.label, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(Palette.ON_ACCENT),
                        selectedTextColor = Color(Palette.AMBER),
                        indicatorColor = Color(Palette.AMBER),
                        unselectedIconColor = Color(Palette.INK_DIM),
                        unselectedTextColor = Color(Palette.INK_DIM),
                    ),
                )
            }
        }
    }
}
