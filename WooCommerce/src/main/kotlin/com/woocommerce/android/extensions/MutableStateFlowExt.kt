package com.woocommerce.android.extensions

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Updates the [MutableStateFlow.value] atomically using the specified [function] of its value.
 *
 * [function] may be evaluated multiple times, if [value] is being concurrently updated.
 *
 * // TODO remove this extension when we update to Kotlin 1.5, since it's part of it
 */
inline fun <T> MutableStateFlow<T>.update(function: (T) -> T) {
    while (true) {
        val prevValue = value
        val nextValue = function(prevValue)
        if (compareAndSet(prevValue, nextValue)) {
            return
        }
    }
}
