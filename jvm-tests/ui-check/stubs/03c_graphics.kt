@file:Suppress("UNUSED_PARAMETER", "unused")

package androidx.compose.ui.graphics

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

@JvmInline value class Color(val value: ULong) {
    fun copy(alpha: Float = 0f, red: Float = 0f, green: Float = 0f, blue: Float = 0f): Color = TODO()

    companion object {
        val Transparent: Color = TODO()
        val White: Color = TODO()
        val Black: Color = TODO()
        val Unspecified: Color = TODO()
    }
}
fun Color(color: Int): Color = TODO()
fun Color(color: Long): Color = TODO()
fun Color(red: Int, green: Int, blue: Int, alpha: Int = 0xFF): Color = TODO()

interface Shape
val RectangleShape: Shape = TODO()

abstract class Brush {
    companion object {
        fun verticalGradient(colors: List<Color>, startY: Float = 0f, endY: Float = Float.POSITIVE_INFINITY, tileMode: Int = 0): Brush = TODO()
        fun radialGradient(colors: List<Color>, center: Offset = Offset.Zero, radius: Float = Float.POSITIVE_INFINITY, tileMode: Int = 0): Brush = TODO()
    }
}

interface ImageBitmap
fun android.graphics.Bitmap.asImageBitmap(): ImageBitmap = TODO()

interface Path {
    fun moveTo(x: Float, y: Float)
    fun lineTo(x: Float, y: Float)
    fun close()
}
fun Path(): Path = TODO()

fun Modifier.graphicsLayer(
    scaleX: Float = 1f,
    scaleY: Float = 1f,
    alpha: Float = 1f,
    translationX: Float = 0f,
    translationY: Float = 0f,
): Modifier = TODO()
