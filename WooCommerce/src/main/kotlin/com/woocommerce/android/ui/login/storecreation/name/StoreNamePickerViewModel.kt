package com.woocommerce.android.ui.login.storecreation.name

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_STEP
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_STEP_STORE_NAME
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class StoreNamePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore,
    val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    private val _storeName = savedState.getStateFlow(scope = this, initialValue = "")
    val storeName: LiveData<String> = _storeName.asLiveData()

    init {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                KEY_STEP to VALUE_STEP_STORE_NAME
            )
        )
    }

    fun onCancelPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onStoreNameChanged(newName: String) {
        _storeName.value = newName
    }

    fun onContinueClicked() {
        newStore.store.update {
            it.copy(name = storeName.value)
        }
        triggerEvent(NavigateToNextStep)
    }

    object NavigateToNextStep : MultiLiveEvent.Event()
}
