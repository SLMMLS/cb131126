@file:Suppress("UNUSED_PARAMETER", "unused")

package androidx.compose.ui.geometry

@JvmInline value class Offset(val packed: Long) {
    val x: Float get() = TODO()
    val y: Float get() = TODO()
    operator fun plus(other: Offset): Offset = TODO()

    companion object { val Zero: Offset = Offset(0) }
}
fun Offset(x: Float, y: Float): Offset = TODO()

@JvmInline value class Size(val packed: Long) {
    val width: Float get() = TODO()
    val height: Float get() = TODO()
    val minDimension: Float get() = TODO()
}
