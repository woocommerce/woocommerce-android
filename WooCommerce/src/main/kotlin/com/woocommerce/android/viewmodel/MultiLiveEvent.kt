package com.woocommerce.android.viewmodel

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A lifecycle-aware observable that sends only new updates after subscription, used for events like
 * navigation and Snackbar messages.
 *
 *
 * This avoids a common problem with events: on configuration change (like rotation) an update
 * can be emitted if the observer is active. This LiveData only calls the observable if there's an
 * explicit call to setValue() or call().
 *
 *
 * This is a mutation of SingleLiveEvent, which allows multiple observers. Once an observer marks the event as handled,
 * no other observers are notified and no further updates will be sent, similar to SingleLiveEvent.
 */
open class MultiLiveEvent<T : IEvent> : MutableLiveData<T>() {
    companion object {
        private const val TAG = "MultiLiveEvent"
    }

    private val pending = AtomicBoolean(false)

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        // Observe the internal MutableLiveData
        super.observe(owner, Observer { t ->
            if (pending.get()) {
                t.isHandled = true
                observer.onChanged(t)
                pending.compareAndSet(t.isHandled, false)
            }
        })
    }

    fun reset() {
        pending.set(false)
    }

    @MainThread
    override fun setValue(t: T?) {
        pending.set(true)
        super.setValue(t)
    }

    override fun postValue(value: T) {
        pending.set(true)
        super.postValue(value)
    }
}
