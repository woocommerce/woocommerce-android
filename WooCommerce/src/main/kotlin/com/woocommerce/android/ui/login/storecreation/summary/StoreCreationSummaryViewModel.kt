package com.woocommerce.android.ui.login.storecreation.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.notifications.local.LocalNotification.StoreCreationFinishedNotification
import com.woocommerce.android.notifications.local.LocalNotificationScheduler
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Failed
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Finished
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Loading
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.util.IsRemoteFeatureFlagEnabled
import com.woocommerce.android.util.RemoteFeatureFlag.LOCAL_NOTIFICATION_STORE_CREATION_READY
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

@HiltViewModel
class StoreCreationSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore,
    private val createStore: CreateFreeTrialStore,
    private val tracker: AnalyticsTrackerWrapper,
    private val localNotificationScheduler: LocalNotificationScheduler,
    private val isRemoteFeatureFlagEnabled: IsRemoteFeatureFlagEnabled,
    private val accountStore: AccountStore
) : ScopedViewModel(savedStateHandle) {
    private val _isLoading = savedStateHandle.getStateFlow(scope = this, initialValue = false)
    val isLoading = _isLoading.asLiveData()

    init {
        tracker.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_SUMMARY
            )
        )

        val newStoreProfilerData = newStore.data.profilerData
        tracker.track(
            stat = AnalyticsEvent.SITE_CREATION_PROFILER_DATA,
            properties = mapOf(
                AnalyticsTracker.KEY_INDUSTRY_SLUG to newStoreProfilerData?.industryKey,
                AnalyticsTracker.KEY_USER_COMMERCE_JOURNEY to newStoreProfilerData?.userCommerceJourneyKey,
                AnalyticsTracker.KEY_ECOMMERCE_PLATFORMS to newStoreProfilerData?.eCommercePlatformKeys?.joinToString(),
                AnalyticsTracker.KEY_COUNTRY_CODE to newStore.data.country?.code,
            )
        )
    }

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

                        manageDeferredNotifications()
                    }
                    is Failed -> triggerEvent(OnStoreCreationFailure)
                    else -> { /* no op */ }
                }
            }
        }
    }

    private fun manageDeferredNotifications() {
        if (isRemoteFeatureFlagEnabled(LOCAL_NOTIFICATION_STORE_CREATION_READY)) {
            val name = if (accountStore.account.firstName.isNotNullOrEmpty())
                accountStore.account.firstName
            else
                accountStore.account.userName
            localNotificationScheduler.scheduleNotification(StoreCreationFinishedNotification(name))
        }
    }

    object OnCancelPressed : MultiLiveEvent.Event()
    object OnStoreCreationSuccess : MultiLiveEvent.Event()
    object OnStoreCreationFailure : MultiLiveEvent.Event()
}
