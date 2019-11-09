package com.woocommerce.android.viewmodel

import android.os.Parcelable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.lang.IllegalStateException
import kotlin.reflect.KProperty

/**
 *  A wrapper around [MutableLiveData], that creates an entry in the [SavedStateHandle] to preserve the data.
 *  An initial value is required during the initialization.
 *
 *  This delegate can then be used as a proxy to access and modify the LiveData, which looks like a simple
 *  variable manipulation.
 */
class LiveDataDelegate<T : Parcelable>(
    savedState: SavedState,
    initialValue: T,
    savedStateKey: String = initialValue.javaClass.name,
    val onChange: (T) -> Unit = {}
) {
    private val _liveData: MutableLiveData<T> = savedState.getLiveData(savedStateKey, initialValue)
    val liveData: LiveData<T> = _liveData

    private var previousValue: T? = null

    fun observe(owner: LifecycleOwner, observer: (T?, T) -> Unit) {
        _liveData.observe(owner, Observer {
            observer(previousValue, it)
            previousValue = it
        })
    }

    fun observeForever(observer: (T?, T) -> Unit) {
        _liveData.observeForever {
            observer(previousValue, it)
            previousValue = it
        }
    }

    operator fun setValue(ref: Any, p: KProperty<*>, value: T) {
        _liveData.value = value
        onChange(value)
    }

    operator fun getValue(ref: Any, p: KProperty<*>): T = _liveData.value!!

    // This resets the previous values
    // Workaround for the activity ViewModel scope
    fun reset() {
        previousValue = null
    }
}
