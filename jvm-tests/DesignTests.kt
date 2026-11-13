package com.clipsync.jvmtests

import com.clipsync.app.files.Direction
import com.clipsync.app.files.FileCategory
import com.clipsync.app.files.FileSource
import com.clipsync.app.files.FileTypes
import com.clipsync.app.files.StagedSource
import com.clipsync.app.files.TState
import com.clipsync.app.files.ThumbMath
import com.clipsync.app.files.TransferLog
import com.clipsync.app.files.TransferView
import com.clipsync.app.ui.Palette
import java.io.File
import java.io.InputStream

// -- Colour arithmetic for the contrast tests (WCAG 2.x) ------------------------------------------------------------

/** A colour as three channels 0..255, fractional so that layers can be composited exactly. */
private class Rgb(val r: Double, val g: Double, val b: Double)

private fun rgbOf(argb: Long) = Rgb(((argb shr 16) and 0xFF).toDouble(), ((argb shr 8) and 0xFF).toDouble(), (argb and 0xFF).toDouble())

private fun alphaOf(argb: Long): Double = ((argb shr 24) and 0xFF) / 255.0

/** [top] (with its own alpha, times [extra]) painted over [under]. */
private fun over(top: Long, under: Rgb, extra: Double = 1.0): Rgb = overRgb(rgbOf(top), alphaOf(top) * extra, under)

private fun overRgb(top: Rgb, alpha: Double, under: Rgb) = Rgb(
    top.r * alpha + under.r * (1 - alpha),
    top.g * alpha + under.g * (1 - alpha),
    top.b * alpha + under.b * (1 - alpha),
)

private fun channel(c: Double): Double {
    val v = c / 255.0
    return if (v <= 0.04045) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
}

private fun luminance(c: Rgb) = 0.2126 * channel(c.r) + 0.7152 * channel(c.g) + 0.0722 * channel(c.b)

private fun contrast(a: Rgb, b: Rgb): Double {
    val la = luminance(a)
    val lb = luminance(b)
    return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}

private fun fmt(x: Double) = String.format(java.util.Locale.ROOT, "%.2f", x)

/** A file source that only has a name, for the queue test. */
private class PreviewSource(override val name: String, override val previewUri: String?) : FileSource {
    private val bytes = sample(2000, 5)
    override val size: Long get() = bytes.size.toLong()
    override fun open(): InputStream = bytes.inputStream()
}

fun DesignTests() {
    println("Design: file categories and tiles")

    Kit.test("a file is sorted into a category by its extension, whatever the case") {
        eq(FileTypes.categoryOf("photo.JPG"), FileCategory.IMAGE)
        eq(FileTypes.categoryOf("IMG_0001.heic"), FileCategory.IMAGE)
        eq(FileTypes.categoryOf("clip.MP4"), FileCategory.VIDEO)
        eq(FileTypes.categoryOf("clip.3gp"), FileCategory.VIDEO)
        eq(FileTypes.categoryOf("song.flac"), FileCategory.AUDIO)
        eq(FileTypes.categoryOf("report.pdf"), FileCategory.PDF)
        eq(FileTypes.categoryOf("letter.docx"), FileCategory.DOCUMENT)
        eq(FileTypes.categoryOf("budget.xlsx"), FileCategory.SHEET)
        eq(FileTypes.categoryOf("talk.pptx"), FileCategory.SLIDES)
        eq(FileTypes.categoryOf("notes.md"), FileCategory.TEXT)
        eq(FileTypes.categoryOf("backup.tar.gz"), FileCategory.ARCHIVE, "the last extension decides")
        eq(FileTypes.categoryOf("data.bin"), FileCategory.OTHER)
        eq(FileTypes.categoryOf("README"), FileCategory.OTHER)
        eq(FileTypes.categoryOf(".bashrc"), FileCategory.OTHER, "a name that only starts with a dot has no extension")
    }

    Kit.test("what the app never opens is a PROGRAM, even when it would look like something else") {
        for (name in listOf("setup.exe", "app.apk", "run.SH", "installer.msi", "script.js")) {
            eq(FileTypes.categoryOf(name), FileCategory.PROGRAM, name)
        }
        // every extension FileNames calls risky must land there, so that the tile and the "do not open" rule never disagree
        for (ext in listOf("exe", "msi", "bat", "cmd", "scr", "ps1", "vbs", "jar", "dll", "lnk", "apk", "dmg", "iso", "py", "sh")) {
            check(com.clipsync.app.files.FileNames.isRisky("x.$ext"), "test premise: .$ext is risky")
            eq(FileTypes.categoryOf("x.$ext"), FileCategory.PROGRAM, ".$ext")
        }
    }

    Kit.test("only photos and videos ask the system for a thumbnail") {
        val withThumbnail = FileCategory.values().filter { it.hasThumbnail }.toSet()
        eq(withThumbnail, setOf(FileCategory.IMAGE, FileCategory.VIDEO))
    }

    Kit.test("the tile says the extension: capitals, four letters at most, FILE when there is none") {
        eq(FileTypes.tileLabel("report.pdf"), "PDF")
        eq(FileTypes.tileLabel("a.docx"), "DOCX")
        eq(FileTypes.tileLabel("a.webp"), "WEBP")
        eq(FileTypes.tileLabel("movie.torrent"), "TORR")
        eq(FileTypes.tileLabel("README"), "FILE")
        eq(FileTypes.tileLabel(".bashrc"), "FILE")
        eq(FileTypes.tileLabel("файл.ТХТ"), "FILE", "an extension that is not ASCII is not shown as one")
        eq(FileTypes.tileLabel("a.mp3"), "MP3")
        eq(FileTypes.tileLabel("weird.a-b"), "AB", "only letters and digits are kept")
    }

    println("Design: thumbnail arithmetic")

    Kit.test("the sample size shrinks as far as the shorter side allows, in powers of two") {
        eq(ThumbMath.sampleSize(4000, 3000, 200), 8, "3000 / 8 = 375 >= 200, 3000 / 16 = 187 < 200")
        eq(ThumbMath.sampleSize(3000, 4000, 200), 8, "portrait: the same, the shorter side decides")
        eq(ThumbMath.sampleSize(1000, 1000, 200), 4)
        eq(ThumbMath.sampleSize(400, 400, 200), 2)
        eq(ThumbMath.sampleSize(399, 399, 200), 1)
        eq(ThumbMath.sampleSize(100, 100, 200), 1, "a small picture is not shrunk")
        eq(ThumbMath.sampleSize(8000, 100, 200), 1, "a very thin strip must not be shrunk below the target either")
        eq(ThumbMath.sampleSize(0, 100, 200), 1)
        eq(ThumbMath.sampleSize(100, 100, 0), 1)
    }

    Kit.test("the shorter side of a sampled picture never falls below the target") {
        for (w in listOf(321, 640, 1080, 1920, 4032, 7680)) {
            for (h in listOf(240, 480, 1080, 3024, 4320)) {
                for (target in listOf(96, 150, 200, 300)) {
                    val s = ThumbMath.sampleSize(w, h, target)
                    check(s >= 1 && (s and (s - 1)) == 0, "a power of two: $s")
                    if (minOf(w, h) >= target) check(minOf(w, h) / s >= target, "${w}x$h at $target: sample $s leaves ${minOf(w, h) / s}")
                }
            }
        }
    }

    Kit.test("EXIF orientation: the eight values and everything else") {
        val expected = mapOf(
            1 to ThumbMath.Turn(0, false), 2 to ThumbMath.Turn(0, true), 3 to ThumbMath.Turn(180, false), 4 to ThumbMath.Turn(180, true),
            5 to ThumbMath.Turn(90, true), 6 to ThumbMath.Turn(90, false), 7 to ThumbMath.Turn(270, true), 8 to ThumbMath.Turn(270, false),
        )
        for ((tag, turn) in expected) eq(ThumbMath.turnFor(tag), turn, "orientation $tag")
        for (junk in listOf(0, 9, -1, 255)) check(ThumbMath.turnFor(junk).isIdentity, "damaged tag $junk leaves the picture alone")
        check(ThumbMath.turnFor(1).isIdentity)
    }

    Kit.test("the thumbnail cache is a sixteenth of the memory, between 4 and 24 MiB") {
        val mib = 1024L * 1024
        eq(ThumbMath.cacheBytes(64 * mib), (4 * mib).toInt(), "small heap: the floor")
        eq(ThumbMath.cacheBytes(256 * mib), (16 * mib).toInt())
        eq(ThumbMath.cacheBytes(1024 * mib), (24 * mib).toInt(), "big heap: the ceiling")
        eq(ThumbMath.cacheBytes(0), (4 * mib).toInt())
    }

    Kit.test("the same address at two sizes is two cache entries") {
        check(ThumbMath.key("content://a/1", 100) != ThumbMath.key("content://a/1", 200))
        eq(ThumbMath.key("content://a/1", 100), ThumbMath.key("content://a/1", 100))
        check(ThumbMath.key("content://a/1", 100) != ThumbMath.key("content://a/2", 100))
    }

    println("Design: the thumbnail address in the transfer list")

    Kit.test("a thumbnail address survives the saved list; a list from an older version loads without one") {
        val dir = tempDir("preview")
        val file = File(dir, "log.json")
        val log = TransferLog(file)
        val view = TransferView(
            id = "out-1", transferId = "t1", direction = Direction.OUT, name = "photo.jpg", size = 10, state = TState.COMPLETE,
            startedAt = 1, updatedAt = 2, finishedAt = 3, preview = "content://media/picker/0/1",
        )
        log.save(listOf(view, view.copy(id = "out-2", preview = null)))
        val loaded = log.load()
        eq(loaded.map { it.preview }, listOf("content://media/picker/0/1", null))

        // The format of round 5/6: no "preview" key at all.
        file.writeText(
            """{"transfers":[{"id":"out-3","transferId":"t3","direction":"out","name":"a.txt","size":5,"transferred":5,"state":"complete","startedAt":1,"updatedAt":2}]}""",
        )
        val old = log.load().single()
        eq(old.preview, null)
        eq(old.name, "a.txt")
        dir.deleteRecursively()
    }

    Kit.test("a row of the queue carries the address its file can be drawn from; a private copy offers its own path") {
        val rig = ManagerRig()
        val ids = rig.manager.enqueue(listOf(PreviewSource("pic.jpg", "content://picker/7"), PreviewSource("note.txt", null)))
        eq(rig.registry.get(ids[0])?.preview, "content://picker/7")
        eq(rig.registry.get(ids[1])?.preview, null)
        val deadline = System.currentTimeMillis() + 15_000
        while (!rig.settled()) {
            if (System.currentTimeMillis() > deadline) throw AssertionError("the queue did not settle")
            Thread.sleep(10)
        }
        eq(rig.registry.get(ids[0])?.preview, "content://picker/7", "kept through progress and completion")

        val staged = StagedSource(File("/tmp/clipsync-x/3f2a"), "a.jpg")
        eq(staged.previewUri, "file:///tmp/clipsync-x/3f2a")
    }

    println("Design: palette")

    // The surfaces the text can land on. The ground is black with the two glows of Palette.GLOWS on it, and the glass
    // goes on top; the glass is taken at its lightest (the top of its gradient). Instead of guessing a worst case,
    // the ground is measured: every point of a grid over screens of very different shapes, glow centres included.
    val black = rgbOf(Palette.BLACK)

    fun groundAt(x: Double, y: Double, w: Double, h: Double): Rgb {
        var c = black
        for (g in Palette.GLOWS) {
            val distance = Math.hypot(x - g.cx * w, y - g.cy * h)
            val alpha = g.peak * maxOf(0.0, 1.0 - distance / (g.radius * minOf(w, h)))
            if (alpha > 0.0) c = overRgb(rgbOf(g.color), alpha, c)
        }
        return c
    }

    val screens = listOf(360 to 800, 411 to 915, 800 to 360, 600 to 960, 1280 to 800, 1200 to 1600, 320 to 480)
    var lightestGlass = over(Palette.GLASS_TOP, black)
    var lightestGlassWhere = "plain black"
    var lightestBar = over(Palette.BAR, black)
    for ((w, h) in screens) {
        val points = ArrayList<Pair<Double, Double>>()
        for (i in 0..24) for (j in 0..24) points.add(w * i / 24.0 to h * j / 24.0)
        for (g in Palette.GLOWS) points.add(g.cx * w.toDouble() to g.cy * h.toDouble()) // the peaks themselves
        for ((x, y) in points) {
            val ground = groundAt(x, y, w.toDouble(), h.toDouble())
            val glass = over(Palette.GLASS_TOP, ground)
            if (luminance(glass) > luminance(lightestGlass)) {
                lightestGlass = glass
                lightestGlassWhere = "${fmt(x)},${fmt(y)} on ${w}x$h"
            }
            val bar = over(Palette.BAR, ground)
            if (luminance(bar) > luminance(lightestBar)) lightestBar = bar
        }
    }
    val glassOnGlows = listOf("the lightest glass (at $lightestGlassWhere)" to lightestGlass, "glass on black" to over(Palette.GLASS_TOP, black))
    val surfaces: List<Pair<String, Rgb>> = listOf("black" to black) + glassOnGlows + listOf(
        "the lightest bottom bar" to lightestBar,
        "bottom bar on white (the camera view behind a panel)" to over(Palette.BAR, Rgb(255.0, 255.0, 255.0)),
        "solid surface" to rgbOf(Palette.SOLID),
        "solid surface, high" to rgbOf(Palette.SOLID_HIGH),
        "solid surface, highest" to rgbOf(Palette.SOLID_HIGHEST),
        "snackbar" to rgbOf(Palette.SNACKBAR),
    )

    Kit.test("every text colour is readable (4.5:1) on every surface it can land on") {
        val texts = listOf(
            "INK" to Palette.INK, "INK_DIM" to Palette.INK_DIM, "INK_FAINT" to Palette.INK_FAINT,
            "AMBER" to Palette.AMBER, "CYAN" to Palette.CYAN, "DANGER" to Palette.DANGER,
        )
        val summary = StringBuilder()
        for ((textName, text) in texts) {
            var worst = 99.0
            var worstWhere = ""
            for ((surfaceName, surface) in surfaces) {
                val ratio = contrast(rgbOf(text), surface)
                if (ratio < worst) {
                    worst = ratio
                    worstWhere = surfaceName
                }
                check(ratio >= 4.5, "$textName on $surfaceName is ${fmt(ratio)}:1, below 4.5")
            }
            summary.append("$textName ${fmt(worst)}  ")
        }
        println("       (weakest ratio per colour: ${summary.toString().trim()})")
    }

    Kit.test("main text is comfortably above the minimum: 7:1 everywhere") {
        for ((surfaceName, surface) in surfaces) {
            val ratio = contrast(rgbOf(Palette.INK), surface)
            check(ratio >= 7.0, "INK on $surfaceName is ${fmt(ratio)}:1")
        }
    }

    Kit.test("text on a filled accent (button, selected tab) is readable: 7:1") {
        for ((name, fill) in listOf("AMBER" to Palette.AMBER, "CYAN" to Palette.CYAN, "DANGER" to Palette.DANGER)) {
            val ratio = contrast(rgbOf(Palette.ON_ACCENT), rgbOf(fill))
            check(ratio >= 7.0, "ON_ACCENT on $name is ${fmt(ratio)}:1")
        }
    }

    Kit.test("an outline or a switch track in INK_FAINT stands out from black by at least 3:1") {
        check(contrast(rgbOf(Palette.INK_FAINT), black) >= 3.0)
        for ((name, glass) in glassOnGlows) check(contrast(rgbOf(Palette.INK_FAINT), glass) >= 3.0, name)
    }

    Kit.test("the extension on a file tile is readable in its tint, on the tile, on the lightest glass") {
        val lightest = glassOnGlows.map { it.second }.maxByOrNull { luminance(it) }!!
        for (category in FileCategory.values()) {
            val tint = Palette.tileTint(category)
            val tile = overRgb(rgbOf(tint), Palette.TILE_FILL_ALPHA.toDouble(), lightest)
            val ratio = contrast(rgbOf(tint), tile)
            check(ratio >= 4.5, "$category: ${fmt(ratio)}:1")
        }
    }

    Kit.test("the glass is a gradient that is lighter at the top, and so is its edge") {
        check(alphaOf(Palette.GLASS_TOP) > alphaOf(Palette.GLASS_BOTTOM))
        check(alphaOf(Palette.GLASS_EDGE_TOP) > alphaOf(Palette.GLASS_EDGE_BOTTOM))
        // The edge is painted over the glass, so at each end it must be lighter than the glass it surrounds.
        check(alphaOf(Palette.GLASS_EDGE_TOP) > alphaOf(Palette.GLASS_TOP), "top edge lighter than top glass")
        check(alphaOf(Palette.GLASS_EDGE_BOTTOM) > alphaOf(Palette.GLASS_BOTTOM), "bottom edge lighter than bottom glass")
        check(alphaOf(Palette.BAR) in 0.8..0.95, "the bar is nearly, not entirely, opaque")
    }

    Kit.test("every colour is fully opaque unless it is meant to be glass") {
        for ((name, value) in listOf(
            "BLACK" to Palette.BLACK, "INK" to Palette.INK, "INK_DIM" to Palette.INK_DIM, "INK_FAINT" to Palette.INK_FAINT,
            "AMBER" to Palette.AMBER, "CYAN" to Palette.CYAN, "DANGER" to Palette.DANGER, "ON_ACCENT" to Palette.ON_ACCENT,
            "SOLID" to Palette.SOLID, "SOLID_HIGH" to Palette.SOLID_HIGH, "SOLID_HIGHEST" to Palette.SOLID_HIGHEST, "SNACKBAR" to Palette.SNACKBAR,
            "TILE_VIOLET" to Palette.TILE_VIOLET, "TILE_BLUE" to Palette.TILE_BLUE, "TILE_GREEN" to Palette.TILE_GREEN, "TILE_SAND" to Palette.TILE_SAND,
        )) {
            eq(alphaOf(value), 1.0, "$name must have alpha FF (a missing alpha byte makes a colour invisible in Compose)")
        }
    }
}
