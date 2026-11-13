package com.clipsync.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * The look of the app in one file: black ground with two soft glows, grey glass on top of it, bright accents.
 * Screens are built from these pieces and hold no colours of their own (they come from `Palette`, through the theme),
 * so "make the glass a little clearer" or "another accent" is a one-line change in `Palette.kt`.
 */

private val CardShape = RoundedCornerShape(20.dp)
private val ButtonShape = RoundedCornerShape(16.dp)

// The same for every card (a gradient spans whatever size it is drawn at), so made once instead of once per card per frame.
private val GlassFill: Brush = Brush.verticalGradient(listOf(Color(Palette.GLASS_TOP), Color(Palette.GLASS_BOTTOM)))
private val GlassEdge: Brush = Brush.verticalGradient(listOf(Color(Palette.GLASS_EDGE_TOP), Color(Palette.GLASS_EDGE_BOTTOM)))

/** The ground of every screen: black, with the glows of [Palette.GLOWS] on it. Drawn once, nothing here animates. */
@Composable
fun GlassBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(Color(Palette.BLACK))
            .drawWithCache {
                val brushes = Palette.GLOWS.map { glow ->
                    val color = Color(glow.color)
                    Brush.radialGradient(
                        // The far end is the same colour with no opacity (not Color.Transparent, which is black and
                        // would darken the glow on its way out).
                        colors = listOf(color.copy(alpha = glow.peak), color.copy(alpha = 0f)),
                        center = Offset(glow.cx * size.width, glow.cy * size.height),
                        radius = glow.radius * minOf(size.width, size.height),
                    )
                }
                onDrawBehind { brushes.forEach { brush -> drawRect(brush) } }
            },
        content = content,
    )
}

/**
 * A piece of glass: lighter at the top, with a thin light edge. With [onClick] and/or [onLongClick] the whole card
 * is a button (with the ripple of the theme); without them it is just a surface.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val press = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(onClick = { onClick?.invoke() }, onLongClick = onLongClick)
    } else {
        Modifier
    }
    Column(
        modifier
            .clip(CardShape)
            .background(GlassFill)
            .border(1.dp, GlassEdge, CardShape)
            .then(press)
            .padding(contentPadding),
        content = content,
    )
}

/**
 * The frame of a screen: a title row (with a back arrow when [onBack] is given, and [actions] on the right), the
 * [content] below it, and over everything the snackbar and a floating button. The system bars and the bottom tab bar
 * are the shell's business (`AppShell`): by the time a screen gets control, its area already excludes them.
 */
@Composable
fun ScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHost: SnackbarHostState? = null,
    hasFloatingActionButton: Boolean = false,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(title, onBack, actions)
            content()
        }
        // [hasFloatingActionButton] says whether the slot is in use: it also lifts the snackbar clear of the button.
        if (hasFloatingActionButton) {
            Box(Modifier.align(Alignment.BottomEnd).padding(16.dp)) { floatingActionButton() }
        }
        if (snackbarHost != null) {
            SnackbarHost(
                snackbarHost,
                Modifier.align(Alignment.BottomCenter).padding(bottom = if (hasFloatingActionButton) 88.dp else 12.dp),
            )
        }
    }
}

@Composable
private fun ScreenHeader(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(start = if (onBack == null) 20.dp else 8.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Назад") }
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

/** A small label above a group of cards. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.8.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
    )
}

/** A round dot of a colour: who sent something, whether something is on. Never the only carrier of the meaning (words go next to it). */
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

/** A row of glass chips, one of them selected (amber): the filter above the clipboard list. */
@Composable
fun PillRow(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selected
            val shape = RoundedCornerShape(50)
            Box(
                Modifier
                    .heightIn(min = 40.dp)
                    .clip(shape)
                    .background(if (isSelected) Color(Palette.AMBER) else Color(Palette.GLASS_TOP))
                    .border(1.dp, if (isSelected) Color.Transparent else Color(Palette.GLASS_EDGE_TOP), shape)
                    .selectable(selected = isSelected, onClick = { onSelect(index) }, role = Role.Tab)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color(Palette.ON_ACCENT) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

/** The main action of a screen: filled amber, dark text. */
@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled, shape = ButtonShape) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text)
    }
}

/** A secondary action: clear glass with a light edge. */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(Palette.GLASS_TOP),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, Color(Palette.GLASS_EDGE_TOP)),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text)
    }
}

/** An action that cannot be taken back (unpair, delete): coral text on glass, so that it is never the brightest thing on the screen. */
@Composable
fun DangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(Palette.GLASS_TOP),
            contentColor = Color(Palette.DANGER),
        ),
        border = BorderStroke(1.dp, Color(Palette.DANGER).copy(alpha = 0.5f)),
    ) {
        Text(text)
    }
}

/** A line of a settings card: what it is, a sentence about what it does, and the switch. */
@Composable
fun SwitchRow(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** A hairline between the lines of one card. */
@Composable
fun GlassDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 12.dp).height(1.dp).background(Color(Palette.GLASS_EDGE_BOTTOM)))
}
