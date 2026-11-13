package com.clipsync.app.ui

import com.clipsync.app.files.FileCategory

/**
 * Every colour of the phone app, as plain ARGB numbers (0xAARRGGBB). No Compose in this file on purpose: the
 * contrast test (`jvm-tests/DesignTests.kt`) reads these very constants on a plain JVM and works out, for every text
 * colour, how readable it is on the lightest surface it can land on (glass over the brightest spot of a glow).
 *
 * To change the look of the whole app, change this file and run that test: nothing else holds a colour.
 *
 * The look: a black ground, grey glass on top of it, two bright accents. Amber (`AMBER`) and the glass tint are the
 * ones the PC window already uses (`--safelight`, `--glass-bg` in `src/style.css`), so that the two apps look related.
 */
object Palette {
    // -- Ground and text --------------------------------------------------------------------------------------------
    const val BLACK = 0xFF000000L

    /** Main text. */
    const val INK = 0xFFF2F3F5L

    /** Secondary text: sub-lines, descriptions. */
    const val INK_DIM = 0xFFB8BDC7L

    /** Captions and outlines. Still readable (>= 4.5:1 on any surface): it is never "decoration only". */
    const val INK_FAINT = 0xFF9AA0ABL

    // -- Accents ----------------------------------------------------------------------------------------------------
    /** The brand colour: primary buttons, the selected tab, things sent *to* the PC. */
    const val AMBER = 0xFFE8A33DL

    /** The cool counterpart: things that came *from* the PC, information. */
    const val CYAN = 0xFF2EC8DCL

    /** Errors and destructive actions. Light enough to be read as text on black. */
    const val DANGER = 0xFFF59A92L

    /** Text and icons on a filled accent (an amber button, the selected tab). */
    const val ON_ACCENT = 0xFF0B0C0EL

    // -- Glass: translucent white over the ground, so the glow behind shows through ---------------------------------
    /** A card is a vertical gradient from [GLASS_TOP] to [GLASS_BOTTOM]... */
    const val GLASS_TOP = 0x1AFFFFFFL
    const val GLASS_BOTTOM = 0x0DFFFFFFL

    /** ...with a thin light edge that is brighter at the top, as if lit from above. */
    const val GLASS_EDGE_TOP = 0x38FFFFFFL
    const val GLASS_EDGE_BOTTOM = 0x14FFFFFFL

    /** The bottom bar: the PC window's glass colour (`rgba(22,25,31,…)`), nearly opaque so that the list does not show through the labels. */
    const val BAR = 0xE016191FL

    // -- Solid surfaces for what floats above everything (dialogs, menus, the snackbar) -----------------------------
    const val SOLID = 0xFF16191FL
    const val SOLID_HIGH = 0xFF1D2128L
    const val SOLID_HIGHEST = 0xFF262B34L
    const val SNACKBAR = 0xFF2B303AL

    // -- Glow: two soft spots of colour on the black ground; this is what the glass has to refract ------------------
    /**
     * A soft round spot of [color]: opacity [peak] at its centre, fading linearly to nothing at [radius] from it.
     * [cx] and [cy] are fractions of the screen's width and height; [radius] is a fraction of its *shorter* side.
     * The background draws exactly this (`GlassBackground` in `Glass.kt`) and the contrast test scans exactly this,
     * so that "the lightest place the text can land on" is a measurement, not a guess.
     */
    class Glow(val color: Long, val peak: Float, val cx: Float, val cy: Float, val radius: Float)

    /** Amber in the top left corner, cyan in the bottom right: they meet only where both are almost gone. */
    val GLOWS: List<Glow> = listOf(
        Glow(AMBER, peak = 0.16f, cx = 0.15f, cy = 0.05f, radius = 0.9f),
        Glow(CYAN, peak = 0.12f, cx = 0.95f, cy = 0.85f, radius = 1.0f),
    )

    // -- Tints of the file tiles (the extension on a tile is drawn in the tint, the tile is the tint at TILE_FILL_ALPHA) ---
    const val TILE_VIOLET = 0xFFB9A2FFL
    const val TILE_BLUE = 0xFF86B0FFL
    const val TILE_GREEN = 0xFF7BD88FL
    const val TILE_SAND = 0xFFD9B97AL
    const val TILE_FILL_ALPHA = 0.10f

    /** The tint of the tile of a file of [category]. */
    fun tileTint(category: FileCategory): Long = when (category) {
        FileCategory.IMAGE -> AMBER
        FileCategory.VIDEO -> CYAN
        FileCategory.AUDIO -> TILE_VIOLET
        FileCategory.PDF -> DANGER
        FileCategory.DOCUMENT -> TILE_BLUE
        FileCategory.SHEET -> TILE_GREEN
        FileCategory.SLIDES -> TILE_SAND
        FileCategory.TEXT -> INK_DIM
        FileCategory.ARCHIVE -> TILE_SAND
        FileCategory.PROGRAM -> AMBER
        FileCategory.OTHER -> INK_DIM
    }
}
