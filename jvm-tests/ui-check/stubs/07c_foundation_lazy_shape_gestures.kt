@file:Suppress("UNUSED_PARAMETER", "unused")

package androidx.compose.foundation.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

interface LazyItemScope
interface LazyListScope
class LazyListState

@Composable
fun LazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = LazyListState(),
    contentPadding: PaddingValues = androidx.compose.foundation.layout.PaddingValues(androidx.compose.ui.unit.Dp(0f)),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {}

inline fun <T> LazyListScope.items(
    items: List<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit,
) {}
