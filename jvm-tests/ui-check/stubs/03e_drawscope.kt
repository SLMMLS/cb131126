@file:Suppress("UNUSED_PARAMETER", "unused")

package androidx.compose.ui.graphics.drawscope

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Density

interface DrawScope : Density {
    val size: Size
    val center: Offset

    fun drawRect(brush: Brush, topLeft: Offset = Offset.Zero, size: Size = this.size, alpha: Float = 1.0f)
    fun drawLine(color: Color, start: Offset, end: Offset, strokeWidth: Float = 0f, alpha: Float = 1.0f)
    fun drawCircle(color: Color, radius: Float = size.minDimension / 2.0f, center: Offset = this.center, alpha: Float = 1.0f)
    fun drawPath(path: Path, color: Color, alpha: Float = 1.0f)
}
