@file:Suppress("UNUSED_PARAMETER", "unused")

package androidx.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp

@RequiresOptIn
annotation class ExperimentalFoundationApi

class BorderStroke(val width: Dp, val brush: Brush)
fun BorderStroke(width: Dp, color: Color): BorderStroke = TODO()

fun Modifier.background(color: Color, shape: Shape = RectangleShape): Modifier = TODO()
fun Modifier.background(brush: Brush, shape: Shape = RectangleShape, alpha: Float = 1.0f): Modifier = TODO()
fun Modifier.border(width: Dp, color: Color, shape: Shape = RectangleShape): Modifier = TODO()
fun Modifier.border(width: Dp, brush: Brush, shape: Shape): Modifier = TODO()
fun Modifier.border(border: BorderStroke, shape: Shape = RectangleShape): Modifier = TODO()

fun Modifier.clickable(enabled: Boolean = true, onClickLabel: String? = null, role: Role? = null, onClick: () -> Unit): Modifier = TODO()

@ExperimentalFoundationApi
fun Modifier.combinedClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = TODO()

class ScrollState
@Composable fun rememberScrollState(initial: Int = 0): ScrollState = TODO()
fun Modifier.verticalScroll(state: ScrollState, enabled: Boolean = true, reverseScrolling: Boolean = false): Modifier = TODO()

@Composable
fun Image(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = 1f,
) {}

@Composable
fun Canvas(modifier: Modifier, onDraw: DrawScope.() -> Unit) {}
