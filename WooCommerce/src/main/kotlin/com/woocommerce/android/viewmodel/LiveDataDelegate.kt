package com.woocommerce.android.viewmodel

import android.os.Parcelable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import kotlin.reflect.KProperty

class LiveDataDelegate<T : Parcelable>(val liveData: MutableLiveData<T>, initialValue: T) {
    constructor(savedState: SavedStateHandle, initialValue: T) :
            this(savedState.getLiveData(initialValue.javaClass.name, initialValue), initialValue)

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
