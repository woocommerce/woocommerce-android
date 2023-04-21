package com.woocommerce.android.ui.login.storecreation.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Failed
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Finished
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Loading
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreCreationSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore,
    private val createStore: CreateFreeTrialStore,
    private val tracker: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    private val _isLoading = savedStateHandle.getStateFlow(scope = this, initialValue = false)
    val isLoading = _isLoading.asLiveData()

    fun onCancelPressed() { triggerEvent(OnCancelPressed) }

    fun onTryForFreeButtonPressed() {
        tracker.track(AnalyticsEvent.SITE_CREATION_TRY_FOR_FREE_TAPPED)
        launch {
            createStore(
                storeDomain = newStore.data.domain,
                storeName = newStore.data.name,
                profilerData = newStore.data.profilerData,
                countryCode = newStore.data.country?.code
            ).collect { creationState ->
                _isLoading.update { creationState is Loading }
                when (creationState) {
                    is Finished -> {
                        newStore.update(siteId = creationState.siteId)
                        triggerEvent(OnStoreCreationSuccess)
                    }
                    is Failed -> triggerEvent(OnStoreCreationFailure)
                    else -> { /* no op */ }
                }
            }
        }
    }

    object OnCancelPressed : MultiLiveEvent.Event()
    object OnStoreCreationSuccess : MultiLiveEvent.Event()
    object OnStoreCreationFailure : MultiLiveEvent.Event()
}
