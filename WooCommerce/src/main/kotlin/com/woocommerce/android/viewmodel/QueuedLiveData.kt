package com.woocommerce.android.viewmodel

import androidx.lifecycle.MutableLiveData
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * If an observer is not active and the live data value is set
 * then it will drop the value
 * Using this class the values will be queued till the
 * observer becomes active
 */
open class QueuedLiveData<T> : MutableLiveData<T>() {
    private val values: Queue<T> = LinkedList()

    private val isActive = AtomicBoolean(false)

    override fun onActive() {
        isActive.compareAndSet(false, true)
        while (values.isNotEmpty()) {
            setValue(values.poll())
        }
    }

    override fun onInactive() {
        isActive.compareAndSet(true, false)
    }

    override fun setValue(value: T?) {
        if (isActive.get()) {
            super.setValue(value)
        } else {
            values.add(value)
        }
    }
}
