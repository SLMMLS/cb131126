@file:Suppress("UNUSED_PARAMETER", "unused")

package androidx.compose.ui.input.pointer

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density

interface PointerInputScope : Density

fun Modifier.pointerInput(key1: Any?, block: suspend PointerInputScope.() -> Unit): Modifier = TODO()
