package com.clipsync.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Always dark, whatever the system theme says (ROADMAP 4.3): the app is black glass, and a light theme would need a
 * second palette to be anything but a washed-out copy of it. Every colour comes from [Palette]; this only maps them to
 * the roles Material components read, so that a plain `Button`, `Switch` or `AlertDialog` already looks right.
 */
private val ClipSyncColors = darkColorScheme(
    primary = Color(Palette.AMBER),
    onPrimary = Color(Palette.ON_ACCENT),
    primaryContainer = Color(Palette.SOLID_HIGHEST),
    onPrimaryContainer = Color(Palette.AMBER),
    secondary = Color(Palette.CYAN),
    onSecondary = Color(Palette.ON_ACCENT),
    secondaryContainer = Color(Palette.SOLID_HIGHEST),
    onSecondaryContainer = Color(Palette.INK),
    tertiary = Color(Palette.CYAN),
    onTertiary = Color(Palette.ON_ACCENT),
    background = Color(Palette.BLACK),
    onBackground = Color(Palette.INK),
    surface = Color(Palette.BLACK),
    onSurface = Color(Palette.INK),
    surfaceVariant = Color(Palette.SOLID_HIGH),
    onSurfaceVariant = Color(Palette.INK_DIM),
    surfaceTint = Color(Palette.AMBER),
    inverseSurface = Color(Palette.SNACKBAR),
    inverseOnSurface = Color(Palette.INK),
    inversePrimary = Color(Palette.AMBER),
    error = Color(Palette.DANGER),
    onError = Color(Palette.ON_ACCENT),
    errorContainer = Color(Palette.SOLID_HIGH),
    onErrorContainer = Color(Palette.DANGER),
    outline = Color(Palette.INK_FAINT),
    outlineVariant = Color(Palette.GLASS_EDGE_TOP),
    scrim = Color(Palette.BLACK),
    surfaceBright = Color(Palette.SOLID_HIGHEST),
    surfaceDim = Color(Palette.BLACK),
    surfaceContainerLowest = Color(Palette.BLACK),
    surfaceContainerLow = Color(Palette.SOLID),
    surfaceContainer = Color(Palette.SOLID),
    surfaceContainerHigh = Color(Palette.SOLID_HIGH),
    surfaceContainerHighest = Color(Palette.SOLID_HIGHEST),
)

/** The system font, a little heavier where a title is. Styles that are not listed keep Material's defaults. */
private val ClipSyncTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
)

private val ClipSyncShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun ClipSyncTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ClipSyncColors, typography = ClipSyncTypography, shapes = ClipSyncShapes) {
        // Material 3 takes the colour of a plain `Text` from the container it sits in, and the default is *black*;
        // our containers are transparent glass, which gives no colour. So the default is set here, once.
        CompositionLocalProvider(LocalContentColor provides Color(Palette.INK), content = content)
    }
}
