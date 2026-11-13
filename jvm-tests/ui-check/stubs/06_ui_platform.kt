@file:Suppress("UNUSED_PARAMETER", "unused")

package androidx.compose.ui.platform

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.lifecycle.LifecycleOwner

val LocalContext: ProvidableCompositionLocal<android.content.Context> = TODO()
val LocalDensity: ProvidableCompositionLocal<Density> = TODO()
val LocalHapticFeedback: ProvidableCompositionLocal<HapticFeedback> = TODO()
val LocalClipboardManager: ProvidableCompositionLocal<ClipboardManager> = TODO()
val LocalLifecycleOwner: ProvidableCompositionLocal<LifecycleOwner> = TODO()

interface ClipboardManager {
    fun setText(annotatedString: AnnotatedString)
    fun getText(): AnnotatedString?
}
