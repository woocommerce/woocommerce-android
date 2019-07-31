package com.woocommerce.android.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * A base class for ViewModels that use coroutines. The class provides {@link CoroutineScope} for the coroutine
 * builders and their lifecycle is tied to that of the ViewModel's.
 *
 * When the ViewModel is destroyed, the coroutine job is cancelled and any running coroutine tied to it is stopped.
 */
abstract class ScopedViewModel(private val defaultDispatcher: CoroutineDispatcher) : ViewModel(), CoroutineScope {
    protected var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = defaultDispatcher + job

    override fun onCleared() {
        super.onCleared()

        job.cancel()
    }
}
