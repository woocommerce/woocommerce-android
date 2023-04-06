package com.woocommerce.android.ui.login.storecreation.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Failed
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Loading
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class StoreCreationSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore,
    private val createStore: CreateFreeTrialStore
) : ScopedViewModel(savedStateHandle) {
    val isLoading = createStore.state
        .map { it is Loading }
        .asLiveData()

    init {
        launch {
            createStore.state
                .filter { it is Failed }
                .collect { triggerEvent(OnStoreCreationFailure) }
        }
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

    object OnCancelPressed : MultiLiveEvent.Event()
    object OnStoreCreationSuccess : MultiLiveEvent.Event()
    object OnStoreCreationFailure: MultiLiveEvent.Event()
}
