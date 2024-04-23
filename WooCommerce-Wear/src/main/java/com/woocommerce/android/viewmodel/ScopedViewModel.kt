package com.woocommerce.android.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.Closeable
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/**
 * A base class for ViewModels that use coroutines. The class provides {@link CoroutineScope} for the coroutine
 * builders and their lifecycle is tied to that of the ViewModel's.
 *
 * When the ViewModel is destroyed, the coroutine job is cancelled and any running coroutine tied to it is stopped.
 */
abstract class ScopedViewModel(
    protected val savedState: SavedStateHandle,
    closeable: Closeable? = null,
) : ViewModel(closeable), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = viewModelScope.coroutineContext


    /**
     * Convert a [Flow] to [StateFlow].
     *
     * This uses a policy of keeping the upstream active for 5 seconds after disappearance of last collector
     * to avoid restarting the Flow during configuration changes.
     */
    protected fun <T> Flow<T>.toStateFlow(initialValue: T) = stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_TIME),
        initialValue = initialValue
    )

    companion object {
        private const val STOP_TIMEOUT_TIME = 5000L
    }
}
