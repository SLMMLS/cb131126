package com.clipsync.app.files

import java.util.Locale

/**
 * What a file is, for the picture next to its name in the "Файлы" list: a photo or a video gets a real
 * thumbnail (when the system can make one), everything else a tile with its extension on it, tinted by category.
 * (Not [FileKind]: that one is the type of a message of the file protocol.)
 */
enum class FileCategory(
    /** Worth asking the system for a thumbnail. */
    val hasThumbnail: Boolean,
) {
    IMAGE(true),
    VIDEO(true),
    AUDIO(false),
    PDF(false),
    DOCUMENT(false),
    SHEET(false),
    SLIDES(false),
    TEXT(false),
    ARCHIVE(false),

    /** Programs, scripts, installers ([FileNames.isRisky]): shown as such, and never opened from the app. */
    PROGRAM(false),
    OTHER(false),
}

object FileTypes {
    private val IMAGE = setOf("jpg", "jpeg", "jfif", "png", "gif", "webp", "bmp", "heic", "heif", "avif", "tif", "tiff", "dng", "svg", "ico")
    private val VIDEO = setOf("mp4", "m4v", "mkv", "webm", "mov", "avi", "3gp", "3g2", "mpg", "mpeg", "wmv", "flv")
    private val AUDIO = setOf("mp3", "m4a", "wav", "ogg", "oga", "opus", "flac", "aac", "wma", "amr", "mid", "midi")
    private val DOCUMENT = setOf("doc", "docx", "odt", "rtf", "pages")
    private val SHEET = setOf("xls", "xlsx", "ods", "csv", "tsv", "numbers")
    private val SLIDES = setOf("ppt", "pptx", "odp", "key")
    private val TEXT = setOf("txt", "md", "json", "xml", "html", "htm", "log", "yml", "yaml", "ini", "toml", "srt")
    private val ARCHIVE = setOf("zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz", "zst")

    /** The part after the last dot, in lower case; empty when there is none (`README`, `.bashrc`). */
    fun extension(name: String): String = FileNames.splitExtension(name).second.lowercase(Locale.ROOT)

    fun categoryOf(name: String): FileCategory {
        if (FileNames.isRisky(name)) return FileCategory.PROGRAM
        val ext = extension(name)
        return when {
            ext in IMAGE -> FileCategory.IMAGE
            ext in VIDEO -> FileCategory.VIDEO
            ext in AUDIO -> FileCategory.AUDIO
            ext == "pdf" -> FileCategory.PDF
            ext in DOCUMENT -> FileCategory.DOCUMENT
            ext in SHEET -> FileCategory.SHEET
            ext in SLIDES -> FileCategory.SLIDES
            ext in TEXT -> FileCategory.TEXT
            ext in ARCHIVE -> FileCategory.ARCHIVE
            else -> FileCategory.OTHER
        }
    }

    /**
     * The words on the tile: the extension in capitals, at most four characters (`PDF`, `ZIP`, `DOCX`, `WEBP`; a
     * longer one is cut: `TORRENT` -> `TORR`). `FILE` when there is no extension. Only ASCII letters and digits
     * are kept: whatever else follows the last dot is not an extension a person would recognise.
     */
    fun tileLabel(name: String): String {
        val ext = extension(name).filter { it in 'a'..'z' || it in '0'..'9' }.take(4)
        return if (ext.isEmpty()) "FILE" else ext.uppercase(Locale.ROOT)
    }
}
