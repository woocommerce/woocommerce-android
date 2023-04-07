package com.woocommerce.android.ui.login.storecreation.name

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.help.HelpOrigin.STORE_CREATION
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StoreNamePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val prefsWrapper: AppPrefsWrapper
) : ScopedViewModel(savedStateHandle) {
    private val _storeName = savedState.getStateFlow(scope = this, initialValue = "")
    val storeName = _storeName.asLiveData()

    private val canCreateFreeTrialStore
        get() = FeatureFlag.FREE_TRIAL_M2.isEnabled() &&
            FeatureFlag.STORE_CREATION_PROFILER.isEnabled().not()

    init {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_NAME
            )
        )
    }

    fun onCancelPressed() {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_DISMISSED,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_NAME,
                AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_NATIVE,
                AnalyticsTracker.KEY_SOURCE to prefsWrapper.getStoreCreationSource(),
                AnalyticsTracker.KEY_IS_FREE_TRIAL to FeatureFlag.FREE_TRIAL_M2.isEnabled()
            )
        )
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onExitTriggered() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onHelpPressed() {
        triggerEvent(MultiLiveEvent.Event.NavigateToHelpScreen(STORE_CREATION))
    }

    fun onStoreNameChanged(newName: String) {
        _storeName.value = newName
    }

    fun onContinueClicked() {
        newStore.update(name = storeName.value)
        if (canCreateFreeTrialStore) {
            triggerEvent(NavigateToSummary)
        } else if (FeatureFlag.STORE_CREATION_PROFILER.isEnabled()) {
            triggerEvent(NavigateToStoreProfiler)
        } else {
            triggerEvent(NavigateToDomainPicker(_storeName.value))
        }
    }

    data class NavigateToDomainPicker(val domainInitialQuery: String) : MultiLiveEvent.Event()

    object NavigateToStoreInstallation : MultiLiveEvent.Event()

    object NavigateToStoreProfiler : MultiLiveEvent.Event()

    object NavigateToSummary : MultiLiveEvent.Event()

    sealed class StoreNamePickerState {
        data class Contentful(
            val storeName: String,
            val isCreatingStore: Boolean
        ) : StoreNamePickerState()
        data class Error(val type: StoreCreationErrorType) : StoreNamePickerState()
    }
}
