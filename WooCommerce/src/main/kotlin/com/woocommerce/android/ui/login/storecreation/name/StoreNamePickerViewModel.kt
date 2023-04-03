package com.woocommerce.android.ui.login.storecreation.name

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.help.HelpOrigin.STORE_CREATION
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Error
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Loading
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Success
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
import kotlinx.coroutines.flow.mapNotNull

@HiltViewModel
class StoreNamePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore,
    private val createStore: CreateFreeTrialStore,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val prefsWrapper: AppPrefsWrapper
) : ScopedViewModel(savedStateHandle) {
    private val storeName = savedState.getStateFlow(scope = this, initialValue = "")

    val storePickerState = combine(
        storeName, createStore.state
    ) { storeName, createStoreState ->
        when (createStoreState) {
            is Error -> StoreNamePickerState.Error(createStoreState.type)
            is Loading -> StoreNamePickerState.Contentful(storeName, true)
            else -> StoreNamePickerState.Contentful(storeName, false)
        }
    }.asLiveData()

    init {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_NAME
            )
        )

        launch {
            createStore.state
                .mapNotNull { it as? Success }
                .collect {
                    newStore.update(siteId = it.siteId)
                    triggerEvent(NavigateToStoreInstallation)
                }
        }
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
        if (FeatureFlag.FREE_TRIAL_M2.isEnabled()) {
            launch { startFreeTrialSiteCreation() }
        } else {
            triggerEvent(NavigateToDomainPicker(storeName.value))
        }
    }

    private suspend fun startFreeTrialSiteCreation() {
        createStore(
            storeDomain = newStore.data.domain,
            storeName = newStore.data.name
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
