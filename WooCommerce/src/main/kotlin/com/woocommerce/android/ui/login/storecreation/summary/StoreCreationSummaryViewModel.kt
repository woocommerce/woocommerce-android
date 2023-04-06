package com.woocommerce.android.ui.login.storecreation.summary

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@HiltViewModel
class StoreCreationSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore,
    private val createStore: CreateFreeTrialStore
) : ScopedViewModel(savedStateHandle) {
    val viewState = savedState.getStateFlow(
        scope = this,
        initialValue = ViewState(isLoading = false)
    ).asLiveData()

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

    @Parcelize
    data class ViewState(val isLoading: Boolean) : Parcelable

    object OnCancelPressed : MultiLiveEvent.Event()
    object OnStoreCreationSuccess : MultiLiveEvent.Event()
    object OnStoreCreationFailure: MultiLiveEvent.Event()
}
