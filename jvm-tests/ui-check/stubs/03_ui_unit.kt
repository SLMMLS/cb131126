// Stubs of androidx.compose.ui.unit (Dp, TextUnit, Density). Geometry, graphics and draw scope are in 03b..03e: one package per file.
@file:Suppress("UNUSED_PARAMETER", "unused")

package androidx.compose.ui.unit

@JvmInline value class Dp(val value: Float) {
    companion object { val Unspecified: Dp = Dp(Float.NaN) }
}
inline val Int.dp: Dp get() = Dp(this.toFloat())
inline val Double.dp: Dp get() = Dp(this.toFloat())
inline val Float.dp: Dp get() = Dp(this)

@JvmInline value class TextUnit(val raw: Long) {
    companion object { val Unspecified: TextUnit = TextUnit(0) }
}
inline val Int.sp: TextUnit get() = TextUnit(this.toLong())
inline val Double.sp: TextUnit get() = TextUnit(this.toLong())
inline val Float.sp: TextUnit get() = TextUnit(this.toLong())

interface Density {
    fun Dp.toPx(): Float = TODO()
    fun Dp.roundToPx(): Int = TODO()
}
