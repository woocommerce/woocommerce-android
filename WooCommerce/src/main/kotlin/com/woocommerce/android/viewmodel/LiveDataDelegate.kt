package com.woocommerce.android.viewmodel

import android.os.Parcelable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import java.lang.IllegalStateException
import kotlin.reflect.KProperty

/**
 *  A wrapper around [MutableLiveData], that emits previous and new values on update.
 *
 *  When the provided SavedStateHandle is not null, it creates an entry in the [SavedStateHandle] to preserve the data.
 *
 *  !BEWARE! that only data that can't be easily recovered should be stored - e.g. user's input. Storing complete
 *  viewStates wastes device resources and often leads to issue such as TransactionTooLarge crashes.
 *
 *  An initial value is required during the initialization.
 *
 *  This delegate can then be used as a proxy to access and modify the LiveData, which looks like a simple
 *  variable manipulation.
 *
 *  The delegate and its LiveData is intended to be used only by a single observer due to the way previous/new data is
 *  being updated. If there is more than one, an [IllegalStateException] is thrown.
 *
 */
class LiveDataDelegate<T : Parcelable>
@LiveDelegateSavedStateAPI
constructor(
    savedState: SavedStateHandle? = null,
    private val initialValue: T,
    savedStateKey: String = initialValue.javaClass.name,
    private val onChange: (T?, T) -> Unit = { _, _ -> }
) {
    @OptIn(LiveDelegateSavedStateAPI::class)
    constructor(
        initialValue: T,
        onChange: (T?, T) -> Unit = { _, _ -> }
    ) : this(savedState = null, initialValue = initialValue, onChange = onChange)

    private val _liveData: MutableLiveData<T> =
        savedState?.getLiveData(savedStateKey, initialValue) ?: MutableLiveData<T>(initialValue)
    val liveData: LiveData<T> = _liveData

    private var previousValue: T? = null

    val hasInitialValue: Boolean
        get() = _liveData.value == initialValue

    fun observe(owner: LifecycleOwner, observer: (T?, T) -> Unit) {
        if (_liveData.hasActiveObservers()) {
            error("Multiple observers registered but only one is supported.")
        }

        previousValue = null
        _liveData.observe(owner) {
            onChange(previousValue, it)
            observer(previousValue, it)
            previousValue = it
        }
    }

    fun observeForever(observer: (T?, T) -> Unit) {
        _liveData.observeForever {
            onChange(previousValue, it)
            observer(previousValue, it)
            previousValue = it
        }
    }

    operator fun setValue(ref: Any, p: KProperty<*>, value: T) {
        _liveData.value = value
    }

    operator fun getValue(ref: Any, p: KProperty<*>): T = _liveData.value!!
}

@RequiresOptIn(
    message = "This API will cause the data to be saved to the SavedState, and should be used with caution.",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
annotation class LiveDelegateSavedStateAPI
