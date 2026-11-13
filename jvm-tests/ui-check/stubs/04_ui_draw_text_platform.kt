@file:Suppress("UNUSED_PARAMETER", "unused")

package androidx.compose.ui.draw

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density

fun Modifier.clip(shape: Shape): Modifier = TODO()
fun Modifier.drawBehind(onDraw: DrawScope.() -> Unit): Modifier = TODO()
fun Modifier.drawWithCache(onBuildDrawCache: CacheDrawScope.() -> DrawResult): Modifier = TODO()

class DrawResult
class CacheDrawScope : Density {
    val size: Size get() = TODO()
    fun onDrawBehind(block: DrawScope.() -> Unit): DrawResult = TODO()
}
