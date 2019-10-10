package com.woocommerce.android.util

import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This subclass of {@link MediatorLiveData} includes the ability to observe other {@code LiveData} objects and react on
 * {@code OnChanged} events from them, as well as adds the ability to delay the propagation of the event by a set
 * amount of time.
 *
 * <p>
 * This class correctly propagates its active/inactive states down to source {@code LiveData}
 * objects after a set delay. There can only be one event delayed at a time. If an event is already delayed when
 * a new one is received, then the previous Job to propagate the event down to the observers is canceled.
 * <p>
 *
 * @param <T> The type of data hold by this instance
 */
class ThrottleLiveData<T> constructor(
    private val offset: Long = 100,
    private val coroutineScope: CoroutineScope,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : MediatorLiveData<T>() {
    private var tempValue: T? = null
    private var currentJob: Job? = null

    override fun postValue(value: T) {
        if (tempValue == null || tempValue != value) {
            currentJob?.cancel()
            currentJob = coroutineScope.launch(backgroundDispatcher) {
                tempValue = value
                delay(offset)
                withContext(mainDispatcher) {
                    tempValue = null
                    super.postValue(value)
                }
            }
        }
    }
}
