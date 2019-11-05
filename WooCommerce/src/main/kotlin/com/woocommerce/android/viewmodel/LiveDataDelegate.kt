package com.woocommerce.android.viewmodel

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
class LiveDataDelegate<T : Any>(private val liveData: MutableLiveData<T>) {
    constructor(savedState: SavedStateHandle, initialValue: T) :
            this(savedState.getLiveData(initialValue.javaClass.name, initialValue))

    val value
        get() = liveData.value

    fun observe(owner: LifecycleOwner, observer: (T) -> Unit): Unit =
            liveData.observe(owner, Observer { observer(it) })

    fun observeForever(observer: (T) -> Unit): Unit = liveData.observeForever(observer)

    operator fun setValue(ref: Any, p: KProperty<*>, value: T) {
        liveData.value = value
    }

    operator fun getValue(ref: Any, p: KProperty<*>): T = liveData.value!!
}
