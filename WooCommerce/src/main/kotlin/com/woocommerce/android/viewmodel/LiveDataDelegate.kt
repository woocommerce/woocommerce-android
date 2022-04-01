package com.woocommerce.android.viewmodel

import android.os.Parcelable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import com.woocommerce.android.extensions.scan
import kotlin.reflect.KProperty

/**
 *  A wrapper around [MutableLiveData], that creates an entry in the [SavedStateHandle] to preserve the data.
 *  An initial value is required during the initialization.
 *
 *  This delegate can then be used as a proxy to access and modify the LiveData, which looks like a simple
 *  variable manipulation.
 *
 *  The delegate and its LiveData is intended to be used only by a single observer due to the way previous/new data is
 *  being updated. If there is more than one, an [IllegalStateException] is thrown.
 *
 */
class LiveDataDelegate<T : Parcelable>(
    savedState: SavedStateHandle,
    private val initialValue: T,
    savedStateKey: String = initialValue.javaClass.name,
    private val onChange: (T?, T) -> Unit = { _, _ -> }
) {
    private val _liveData: MutableLiveData<T> = savedState.getLiveData(savedStateKey, initialValue)
    private val previousValueLiveData: LiveData<Pair<T?, T>> = _liveData.scan(null) { accumulatedValue, nextValue ->
        val previousValue = accumulatedValue?.second
        onChange(previousValue, nextValue)
        Pair(previousValue, nextValue)
    }

    val liveData = previousValueLiveData.map { it.second }

    val hasInitialValue: Boolean
        get() = _liveData.value == initialValue

    fun observe(owner: LifecycleOwner, observer: (T?, T) -> Unit) {
        previousValueLiveData.observe(owner) { (previousValue, nextValue) ->
            observer(previousValue, nextValue)
        }
    }

    fun observeForever(observer: (T?, T) -> Unit) {
        previousValueLiveData.observeForever { (previousValue, nextValue) ->
            observer(previousValue, nextValue)
        }
    }

    operator fun setValue(ref: Any?, p: KProperty<*>, value: T) {
        _liveData.value = value
    }

    operator fun getValue(ref: Any?, p: KProperty<*>): T = _liveData.value!!
}
