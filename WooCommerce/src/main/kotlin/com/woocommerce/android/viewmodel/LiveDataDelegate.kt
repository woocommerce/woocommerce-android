package com.woocommerce.android.viewmodel

import android.os.Parcelable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import kotlin.reflect.KProperty

/**
 *  A wrapper around [MutableLiveData], that creates an entry in the [SavedStateHandle] to preserve the data.
 *  An initial value is required during the initialization.
 *
 *  This delegate can then be used as a proxy to access and modify the LiveData, which looks like a simple
 *  variable manipulation.
 */
class LiveDataDelegate<T : Parcelable>(val liveData: MutableLiveData<T>, initialValue: T) {
    constructor(savedState: SavedStateHandle, initialValue: T, savedStateKey: String = initialValue.javaClass.name) :
            this(savedState.getLiveData(savedStateKey, initialValue), initialValue)

    init {
        liveData.value = initialValue
    }

    private var previousValue: T? = null
    val value: T
        get() = liveData.value!!

    fun observe(owner: LifecycleOwner, observer: (T?, T) -> Unit) {
        liveData.observe(owner, Observer {
            observer(previousValue, it)
            previousValue = it
        })
    }

    fun observeForever(observer: (T?, T) -> Unit) {
        liveData.observeForever {
            observer(previousValue, it)
            previousValue = it
        }
    }

    operator fun setValue(ref: Any, p: KProperty<*>, value: T) {
        liveData.value = value
    }

    operator fun getValue(ref: Any, p: KProperty<*>): T = liveData.value!!

    // This resets the previous values
    // Workaround for the activity ViewModel scope
    fun reset() {
        previousValue = null
    }
}
