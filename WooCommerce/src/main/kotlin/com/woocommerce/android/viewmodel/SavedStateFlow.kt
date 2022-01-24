package com.woocommerce.android.viewmodel

import android.os.Parcelable
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
fun <T : Parcelable> SavedStateHandle.getStateFlow(
    scope: CoroutineScope,
    initialValue: T,
    key: String = initialValue.javaClass.name
): MutableStateFlow<T> = this.let { handle ->
    val liveData = handle.getLiveData(key, initialValue).also { liveData ->
        if (liveData.value === initialValue) {
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
    mutableStateFlow
}
