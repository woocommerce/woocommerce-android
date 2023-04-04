package com.woocommerce.android.ui.login.storecreation.name

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.help.HelpOrigin.STORE_CREATION
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Failed
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Loading
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.filterNotNull

@HiltViewModel
class StoreNamePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore,
    private val createStore: CreateFreeTrialStore,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val prefsWrapper: AppPrefsWrapper
) : ScopedViewModel(savedStateHandle) {
    private val storeName = savedState.getStateFlow(scope = this, initialValue = "")

    private val canCreateFreeTrialStore
        get() = FeatureFlag.FREE_TRIAL_M2.isEnabled() &&
            FeatureFlag.STORE_CREATION_PROFILER.isEnabled().not()

    val storePickerState = combine(
        storeName,
        createStore.state,
        ::mapStoreNamePickerState
    ).asLiveData()

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
                AnalyticsTracker.KEY_SOURCE to prefsWrapper.getStoreCreationSource()
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
        storeName.value = newName
    }

    fun onContinueClicked() {
        newStore.update(name = storeName.value)
        if (canCreateFreeTrialStore) {
            launch { startFreeTrialSiteCreation() }
        } else if (FeatureFlag.STORE_CREATION_PROFILER.isEnabled()) {
            triggerEvent(NavigateToDomainPicker(storeName.value))
        } else {
            triggerEvent(NavigateToDomainPicker(storeName.value))
        }
    }

    private suspend fun startFreeTrialSiteCreation() {
        createStore(
            storeDomain = newStore.data.domain,
            storeName = newStore.data.name
        ).filterNotNull().collect {
            newStore.update(siteId = it)
            triggerEvent(NavigateToStoreInstallation)
        }
    }

    private fun mapStoreNamePickerState(
        storeName: String,
        createStoreState: StoreCreationState
    ) = when (createStoreState) {
        is Failed -> StoreNamePickerState.Error(createStoreState.type)
        else -> StoreNamePickerState.Contentful(
            storeName = storeName,
            isCreatingStore = createStoreState is Loading
        )
    }

    data class NavigateToDomainPicker(val domainInitialQuery: String) : MultiLiveEvent.Event()

    object NavigateToStoreInstallation : MultiLiveEvent.Event()

    sealed class StoreNamePickerState {
        data class Contentful(
            val storeName: String,
            val isCreatingStore: Boolean
        ) : StoreNamePickerState()
        data class Error(val type: StoreCreationErrorType) : StoreNamePickerState()
    }
}
