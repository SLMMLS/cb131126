@file:Suppress("UNUSED_PARAMETER", "unused")

package androidx.compose.ui.text.style

@JvmInline value class TextAlign(val value: Int) {
    companion object {
        val Start: TextAlign = TextAlign(1)
        val Center: TextAlign = TextAlign(3)
        val End: TextAlign = TextAlign(2)
    }
}

@JvmInline value class TextOverflow(val value: Int) {
    companion object {
        val Clip: TextOverflow = TextOverflow(1)
        val Ellipsis: TextOverflow = TextOverflow(2)
    }
}
