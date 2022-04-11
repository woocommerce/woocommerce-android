package com.woocommerce.android.viewmodel

import androidx.lifecycle.*
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlin.coroutines.CoroutineContext

/**
 * A base class for ViewModels that use coroutines. The class provides {@link CoroutineScope} for the coroutine
 * builders and their lifecycle is tied to that of the ViewModel's.
 *
 * When the ViewModel is destroyed, the coroutine job is cancelled and any running coroutine tied to it is stopped.
 */
abstract class ScopedViewModel(
    protected val savedState: SavedStateHandle
) : ViewModel(), CoroutineScope {
    protected open val _event: MutableLiveData<Event> = MultiLiveEvent()
    open val event: LiveData<Event> = _event

    override val coroutineContext: CoroutineContext
        get() = viewModelScope.coroutineContext

    protected fun triggerEvent(event: Event) {
        event.isHandled = false
        _event.value = event
    }

    /**
     * Convert a [Flow] to [StateFlow].
     *
     * This uses a policy of keeping the upstream active for 5 seconds after disappearance of last collector
     * to avoid restarting the Flow during configuration changes.
     */
    @Suppress("MagicNumber")
    protected fun <T> Flow<T>.toStateFlow(initialValue: T) = stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = initialValue
    )
}
