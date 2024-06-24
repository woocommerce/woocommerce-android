package com.woocommerce.android.wear.extensions

import android.os.Parcelable
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable

const val DEFAULT_TIMEOUT_MILLIS = 20000L
private fun createTimeoutFlow(
    timeoutMillis: Long
): Flow<Boolean> = flow {
    emit(false)
    delay(timeoutMillis)
    emit(true)
}

fun <T, R> Flow<T>.combineWithTimeout(
    timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    transform: (data: T, isTimeout: Boolean) -> R
) = combine(this, createTimeoutFlow(timeoutMillis), transform)

/**
 * A helper function to create a [MutableStateFlow] that creates an entry in [SavedStateHandle] to persist value
 * through process-death.
 *
 * Based on https://gist.github.com/marcellogalhardo/2a1ec56b7d00ba9af1ec9fd3583d53dc
 *
 * @param scope The scope used to synchronize the [StateFlow] and [SavedStateHandle]
 * @param initialValue If no value exists with the given [key], a new one is created
 *  with the given [initialValue].
 *  @param key an optional key for the value
 */
fun <T : Any> SavedStateHandle.getStateFlow(
    scope: CoroutineScope,
    initialValue: T,
    key: String = initialValue.javaClass.name
): MutableStateFlow<T> {
    if (initialValue !is Parcelable && initialValue !is Serializable) {
        error("getStateFlow supports only types that are either Parcelable or Serializable")
    }

    return getStateFlowInternal(scope, initialValue, key)
}

private fun <T : Any?> SavedStateHandle.getStateFlowInternal(
    scope: CoroutineScope,
    initialValue: T,
    key: String
): MutableStateFlow<T> {
    val liveData = this.getLiveData(key, initialValue).also { liveData ->
        if (initialValue != null && liveData.value === initialValue) {
            // this is a false positive, we are already checking that the value is non-null before assigning it
            @Suppress("NullSafeMutableLiveData")
            liveData.value = initialValue
        }
    }
    val mutableStateFlow = MutableStateFlow(liveData.value ?: initialValue)

    val observer: Observer<T> = Observer { value ->
        mutableStateFlow.value = value
    }
    liveData.observeForever(observer)

    scope.launch {
        mutableStateFlow.also { flow ->
            flow.onCompletion {
                withContext(Dispatchers.Main.immediate) {
                    liveData.removeObserver(observer)
                }
            }.collect { value ->
                withContext(Dispatchers.Main.immediate) {
                    if (liveData.value != value) {
                        liveData.value = value
                    }
                }
            }
        }
    }
    return mutableStateFlow
}
