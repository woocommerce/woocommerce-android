package com.woocommerce.android.ui.login.storecreation.name

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.help.HelpOrigin.STORE_CREATION
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.SITE_ADDRESS_ALREADY_EXISTS
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository
import com.woocommerce.android.ui.login.storecreation.StoreCreationResult
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.flow.combine

@HiltViewModel
class StoreNamePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore,
    private val repository: StoreCreationRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val prefsWrapper: AppPrefsWrapper
) : ScopedViewModel(savedStateHandle) {
    private val storeName = savedState.getStateFlow(scope = this, initialValue = "")
    private val isCreatingStore = savedState.getStateFlow(scope = this, initialValue = false)
    private val error = MutableStateFlow<StoreCreationErrorType?>(null)

    val storePickerState: LiveData<StoreNamePickerState> = combine(
        storeName,
        isCreatingStore,
        error
    ) { storeName, isCreatingStore, error ->
        error?.let { StoreNamePickerState.Error(it) }
            ?: StoreNamePickerState.Contentful(storeName, isCreatingStore)
    }.asLiveData()

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
        launch {
            isCreatingStore.value = true
            newStore.update(name = storeName.value)
            createFreeTrialSite().ifSuccessfulThen {
                newStore.update(siteId = it)
                isCreatingStore.value = false
                triggerEvent(NavigateToNextStep(storeName.value))
            }
        }
    }

    private suspend fun createFreeTrialSite(): StoreCreationResult<Long> {
        suspend fun StoreCreationResult<Long>.recoverIfSiteExists(): StoreCreationResult<Long> {
            return if ((this as? StoreCreationResult.Failure<Long>)?.type == SITE_ADDRESS_ALREADY_EXISTS) {
                repository.getSiteByUrl(newStore.data.domain)?.let { site ->
                    StoreCreationResult.Success(site.siteId)
                } ?: this
            } else {
                this
            }
        }

        return repository.createNewFreeTrialSite(
            StoreCreationRepository.SiteCreationData(
                siteDesign = PlansViewModel.NEW_SITE_THEME,
                domain = newStore.data.domain,
                title = newStore.data.name,
                segmentId = null
            ),
            PlansViewModel.NEW_SITE_LANGUAGE_ID,
            TimeZone.getDefault().id
        ).recoverIfSiteExists()
    }

    private suspend fun <T : Any?> StoreCreationResult<T>.ifSuccessfulThen(
        successAction: suspend (T) -> Unit
    ) {
        when (this) {
            is StoreCreationResult.Success -> successAction(this.data)
            is StoreCreationResult.Failure -> {
                error.emit(this.type)
            }
        }
    }

    data class NavigateToNextStep(val domainInitialQuery: String) : MultiLiveEvent.Event()

    sealed class StoreNamePickerState {
        data class Contentful(
            val storeName: String,
            val isCreatingStore: Boolean
        ) : StoreNamePickerState()
        data class Error(val type: StoreCreationErrorType) : StoreNamePickerState()
    }
}
