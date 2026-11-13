package com.clipsync.app.files

/**
 * The arithmetic of making a thumbnail, kept apart from Android so that it is tested on a plain JVM.
 * (The code that touches the system - `ui/Thumbnails.kt` - only calls these.)
 */
object ThumbMath {
    /**
     * By how much (a power of two, as `BitmapFactory` wants it) a [width] x [height] picture can be shrunk while
     * its *shorter* side stays at least [targetPx]: the thumbnail is cropped to a square, so the shorter side is
     * the one that must not be left short. Never below 1; a picture smaller than the target is not shrunk.
     */
    fun sampleSize(width: Int, height: Int, targetPx: Int): Int {
        if (width <= 0 || height <= 0 || targetPx <= 0) return 1
        val shorter = minOf(width, height)
        var sample = 1
        while (shorter / (sample * 2) >= targetPx) sample *= 2
        return sample
    }

    /** What to do to a decoded picture so that it stands upright: turn clockwise by [degrees], then mirror left-right if [mirror]. */
    data class Turn(val degrees: Int, val mirror: Boolean) {
        val isIdentity: Boolean get() = degrees == 0 && !mirror
    }

    /**
     * The EXIF orientation tag (1..8) as a [Turn]. The same table as in common image libraries: 2 mirrors; 3 turns by
     * 180; 4 turns by 180 and mirrors; 5 turns by 90 and mirrors; 6 turns by 90; 7 turns by 270 and mirrors; 8 turns
     * by 270. Anything else (a missing or damaged tag, 1 included) leaves the picture as it is.
     */
    fun turnFor(exifOrientation: Int): Turn = when (exifOrientation) {
        2 -> Turn(0, true)
        3 -> Turn(180, false)
        4 -> Turn(180, true)
        5 -> Turn(90, true)
        6 -> Turn(90, false)
        7 -> Turn(270, true)
        8 -> Turn(270, false)
        else -> Turn(0, false)
    }

    /** A sixteenth of the memory the app may use, but between 4 and 24 MiB: enough for a screenful of tiles, never a burden. */
    fun cacheBytes(maxMemory: Long): Int {
        val sixteenth = maxMemory / 16
        return sixteenth.coerceIn(4L * 1024 * 1024, 24L * 1024 * 1024).toInt()
    }

    /** The same address at two sizes is two thumbnails. */
    fun key(uri: String, px: Int): String = "$px:$uri"
}
