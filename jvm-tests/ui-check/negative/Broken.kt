// Planted mistakes: ui-check/run.sh must reject this file (4 errors). If it is ever accepted, the check is worthless.
package com.clipsync.app.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box

@Composable
fun Broken(count: Int) {
    // 1. a parameter that does not exist
    Text("hello", colour = androidx.compose.ui.graphics.Color.White)
    // 2. an unresolved name
    Text(undefinedName)
    // 3. a wrong type: Int where String is wanted
    Text(count)
    // 4. an experimental API used without opting in
    Box(Modifier.combinedClickable(onClick = {})) {}
}
