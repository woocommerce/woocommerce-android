package com.woocommerce.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

/**
 * A base class for ViewModels that use coroutines, and which are injected using vanilla Dagger.
 * The class provides {@link CoroutineScope} for the coroutine
 * builders and their lifecycle is tied to that of the ViewModel's.
 *
 * When the ViewModel is destroyed, the coroutine job is cancelled and any running coroutine tied to it is stopped.
 */
abstract class DaggerScopedViewModel(
    protected val savedState: SavedStateWithArgs,
    protected val dispatchers: CoroutineDispatchers
) : ViewModel(), CoroutineScope {
    private val _event = MultiLiveEvent<Event>()
    val event: LiveData<Event> = _event

    override val coroutineContext: CoroutineContext
        get() = viewModelScope.coroutineContext

    protected fun triggerEvent(event: Event) {
        event.isHandled = false
        _event.value = event
    }
}
