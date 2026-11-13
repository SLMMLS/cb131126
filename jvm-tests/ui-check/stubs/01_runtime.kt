// Stubs of androidx.compose.runtime (Compose BOM 2024.09.00): only what the screens use, with the real signatures.
@file:Suppress("UNUSED_PARAMETER", "unused", "NOTHING_TO_INLINE")

package androidx.compose.runtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KProperty

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
annotation class Composable

interface State<out T> { val value: T }
interface MutableState<T> : State<T> { override var value: T }

fun <T> mutableStateOf(value: T): MutableState<T> = TODO()
inline operator fun <T> State<T>.getValue(thisObj: Any?, property: KProperty<*>): T = value
inline operator fun <T> MutableState<T>.setValue(thisObj: Any?, property: KProperty<*>, value: T) { this.value = value }

@Composable inline fun <T> remember(calculation: () -> T): T = calculation()
@Composable inline fun <T> remember(key1: Any?, calculation: () -> T): T = calculation()
@Composable inline fun <T> remember(key1: Any?, key2: Any?, calculation: () -> T): T = calculation()

@Composable fun <T> rememberUpdatedState(newValue: T): State<T> = TODO()
@Composable fun rememberCoroutineScope(): CoroutineScope = TODO()

@Composable fun LaunchedEffect(key1: Any?, block: suspend CoroutineScope.() -> Unit) {}
@Composable fun LaunchedEffect(key1: Any?, key2: Any?, block: suspend CoroutineScope.() -> Unit) {}

interface ProduceStateScope<T> : MutableState<T>, CoroutineScope {
    suspend fun awaitDispose(onDispose: () -> Unit): Nothing
}
@Composable fun <T> produceState(initialValue: T, key1: Any?, producer: suspend ProduceStateScope<T>.() -> Unit): State<T> = TODO()
@Composable fun <T> produceState(initialValue: T, key1: Any?, key2: Any?, producer: suspend ProduceStateScope<T>.() -> Unit): State<T> = TODO()
@Composable fun <T> produceState(initialValue: T, producer: suspend ProduceStateScope<T>.() -> Unit): State<T> = TODO()

@Composable fun <T : R, R> Flow<T>.collectAsState(initial: R, context: CoroutineContext = EmptyCoroutineContext): State<R> = TODO()
@Composable fun <T> StateFlow<T>.collectAsState(context: CoroutineContext = EmptyCoroutineContext): State<T> = TODO()

abstract class CompositionLocal<T> {
    val current: T @Composable get() = TODO()
}
abstract class ProvidableCompositionLocal<T> : CompositionLocal<T>() {
    infix fun provides(value: T): ProvidedValue<T> = TODO()
}
class ProvidedValue<T>
@Composable fun CompositionLocalProvider(vararg values: ProvidedValue<*>, content: @Composable () -> Unit) {}
