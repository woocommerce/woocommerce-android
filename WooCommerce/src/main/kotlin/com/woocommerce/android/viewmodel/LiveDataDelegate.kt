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
class LiveDataDelegate<T : Parcelable> (
    savedState: SavedStateHandle? = null,
    private val initialValue: T,
    savedStateKey: String = initialValue.javaClass.name,
    private val onChange: (T?, T) -> Unit = { _, _ -> }
) {
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

/**
 * !BEWARE! that only data that can't be easily recovered should be stored - e.g. user's input. Storing complete
 *  viewStates wastes device resources and often leads to issue such as TransactionTooLarge crashes.
 *
 *  Note: This method is technically not necessary, we could use the primary constructor. However, we are
 *  misusing the savedState in tons of places in the codebase. This approach will hopefully at least increase
 *  awareness of the associated risks and ensure developers who are using the saved state consider other options.
 *
 *  Note 2: We are intentionally not changing all the existing location to use this factory method as all these
 *  locations should be changed only after considering whether they truly store the minimal required data.
 */
fun <T : Parcelable> createLiveDataDelegateWithSavedState(
    savedState: SavedStateHandle,
    initialValue: T,
    savedStateKey: String = initialValue.javaClass.name,
    onChange: (T?, T) -> Unit = { _, _ -> }
): LiveDataDelegate<T> {
    return LiveDataDelegate(savedState, initialValue, savedStateKey, onChange)
}
