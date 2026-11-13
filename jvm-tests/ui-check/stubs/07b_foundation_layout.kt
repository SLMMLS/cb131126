@file:Suppress("UNUSED_PARAMETER", "unused")

package androidx.compose.foundation.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

interface BoxScope {
    fun Modifier.align(alignment: Alignment): Modifier
    fun Modifier.matchParentSize(): Modifier
}
interface ColumnScope {
    fun Modifier.weight(weight: Float, fill: Boolean = true): Modifier
    fun Modifier.align(alignment: Alignment.Horizontal): Modifier
}
interface RowScope {
    fun Modifier.weight(weight: Float, fill: Boolean = true): Modifier
    fun Modifier.align(alignment: Alignment.Vertical): Modifier
}

object Arrangement {
    interface Horizontal
    interface Vertical
    interface HorizontalOrVertical : Horizontal, Vertical

    val Start: Horizontal = TODO()
    val End: Horizontal = TODO()
    val Top: Vertical = TODO()
    val Bottom: Vertical = TODO()
    val Center: HorizontalOrVertical = TODO()
    fun spacedBy(space: Dp): HorizontalOrVertical = TODO()
}

interface PaddingValues
fun PaddingValues(all: Dp): PaddingValues = TODO()
fun PaddingValues(horizontal: Dp = 0.dp, vertical: Dp = 0.dp): PaddingValues = TODO()
fun PaddingValues(start: Dp = 0.dp, top: Dp = 0.dp, end: Dp = 0.dp, bottom: Dp = 0.dp): PaddingValues = TODO()

// The empty box, used to draw a background or a dot: it exists in the real foundation.layout.
@Composable fun Box(modifier: Modifier) {}

@Composable
fun Box(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {}

@Composable
fun Column(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {}

@Composable
fun Row(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit,
) {}

@Composable fun Spacer(modifier: Modifier) {}

fun Modifier.fillMaxSize(fraction: Float = 1f): Modifier = TODO()
fun Modifier.fillMaxWidth(fraction: Float = 1f): Modifier = TODO()
fun Modifier.height(height: Dp): Modifier = TODO()
fun Modifier.heightIn(min: Dp = Dp.Unspecified, max: Dp = Dp.Unspecified): Modifier = TODO()
fun Modifier.width(width: Dp): Modifier = TODO()
fun Modifier.size(size: Dp): Modifier = TODO()
fun Modifier.size(width: Dp, height: Dp): Modifier = TODO()
fun Modifier.padding(horizontal: Dp = 0.dp, vertical: Dp = 0.dp): Modifier = TODO()
fun Modifier.padding(start: Dp = 0.dp, top: Dp = 0.dp, end: Dp = 0.dp, bottom: Dp = 0.dp): Modifier = TODO()
fun Modifier.padding(all: Dp): Modifier = TODO()
fun Modifier.padding(paddingValues: PaddingValues): Modifier = TODO()
