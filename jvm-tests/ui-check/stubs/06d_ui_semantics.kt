@file:Suppress("UNUSED_PARAMETER", "unused")

package androidx.compose.ui.semantics

@JvmInline value class Role(val value: Int) {
    companion object {
        val Button: Role = Role(0)
        val Tab: Role = Role(3)
    }
}
