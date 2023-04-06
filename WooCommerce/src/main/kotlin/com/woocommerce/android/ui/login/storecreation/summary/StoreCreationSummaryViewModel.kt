package com.woocommerce.android.ui.login.storecreation.summary

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Failed
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Finished
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Idle
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Loading
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@HiltViewModel
class StoreCreationSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore,
    private val createStore: CreateFreeTrialStore
) : ScopedViewModel(savedStateHandle) {
    private val state = savedState.getStateFlow(
        scope = this,
        initialValue = ViewState(isLoading = false)
    )
    val viewState = state.asLiveData()

    init {
        launch { handleStoreCreationStateChanges() }
    }

    fun onCancelPressed() { triggerEvent(OnCancelPressed) }
    fun onTryForFreeButtonPressed() {
        launch {
            createStore(
                storeDomain = newStore.data.domain,
                storeName = newStore.data.name,
            ).collect {
                newStore.update(siteId = it)
                triggerEvent(OnStoreCreationSuccess)
            }
        }
    }

    private suspend fun handleStoreCreationStateChanges() {
        createStore.state.collect { creationState ->
            when(creationState) {
                is Loading ->
                    state.update { it.copy(isLoading = true) }
                is Finished, is Idle ->
                    state.update { it.copy(isLoading = false) }
                is Failed -> {
                    state.update { it.copy(isLoading = false) }
                    triggerEvent(OnStoreCreationFailure)
                }
            }
        }
    }

    @Parcelize
    data class ViewState(val isLoading: Boolean) : Parcelable

    object OnCancelPressed : MultiLiveEvent.Event()
    object OnStoreCreationSuccess : MultiLiveEvent.Event()
    object OnStoreCreationFailure: MultiLiveEvent.Event()
}
