package com.woocommerce.android.extensions

/**
 * Re-throws the encapsulated exception if it is an instance of [E], otherwise returns this [Result] unchanged.
 *
 * Use this when calling [runCatching] around code that may throw [E] for control-flow reasons that must not
 * be swallowed — for example, [kotlinx.coroutines.CancellationException] thrown by [kotlinx.coroutines.flow.FlowCollector.emit]
 * to signal flow cancellation.
 */
inline fun <reified E : Throwable, T> Result<T>.rethrow(): Result<T> = onFailure { if (it is E) throw it }
