@file:Suppress("UNUSED_PARAMETER", "unused")

package androidx.compose.ui.hapticfeedback

interface HapticFeedback { fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) }

@JvmInline value class HapticFeedbackType(val value: Int) {
    companion object {
        val LongPress: HapticFeedbackType = HapticFeedbackType(0)
        val TextHandleMove: HapticFeedbackType = HapticFeedbackType(9)
    }
}
