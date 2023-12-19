package com.woocommerce.android.ui.login.storecreation.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.notifications.local.LocalNotification.FreeTrialExpiredNotification
import com.woocommerce.android.notifications.local.LocalNotification.FreeTrialExpiringNotification
import com.woocommerce.android.notifications.local.LocalNotification.FreeTrialSurveyNotification
import com.woocommerce.android.notifications.local.LocalNotification.StoreCreationCompletedNotification
import com.woocommerce.android.notifications.local.LocalNotification.UpgradeToPaidPlanNotification
import com.woocommerce.android.notifications.local.LocalNotificationScheduler
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Failed
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Finished
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Loading
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.installation.StoreInstallationViewModel
import com.woocommerce.android.util.IsRemoteFeatureFlagEnabled
import com.woocommerce.android.util.RemoteFeatureFlag.LOCAL_NOTIFICATION_1D_AFTER_FREE_TRIAL_EXPIRES
import com.woocommerce.android.util.RemoteFeatureFlag.LOCAL_NOTIFICATION_1D_BEFORE_FREE_TRIAL_EXPIRES
import com.woocommerce.android.util.RemoteFeatureFlag.LOCAL_NOTIFICATION_STORE_CREATION_READY
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class StoreCreationSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore,
    private val createStore: CreateFreeTrialStore,
    private val tracker: AnalyticsTrackerWrapper,
    private val localNotificationScheduler: LocalNotificationScheduler,
    private val isRemoteFeatureFlagEnabled: IsRemoteFeatureFlagEnabled,
    private val accountStore: AccountStore,
    private val resourceProvider: ResourceProvider,
    private val appPrefs: AppPrefsWrapper
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
    }

    fun onCancelPressed() {
        triggerEvent(OnCancelPressed)
    }

    fun onTryForFreeButtonPressed() {
        tracker.track(AnalyticsEvent.SITE_CREATION_TRY_FOR_FREE_TAPPED)
        newStore.update(name = resourceProvider.getString(R.string.store_creation_store_name_default))

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
                        appPrefs.createdStoreSiteId = creationState.siteId
                        tracker.track(
                            stat = AnalyticsEvent.SITE_CREATION_FREE_TRIAL_CREATED_SUCCESS,
                            properties = mapOf(
                                AnalyticsTracker.KEY_NEW_SITE_ID to newStore.data.siteId,
                                AnalyticsTracker.KEY_INITIAL_DOMAIN to newStore.data.domain
                            )
                        )
                        triggerEvent(OnStoreCreationSuccess)

                        manageDeferredNotifications(creationState.siteId)
                    }

                    is Failed -> triggerEvent(OnStoreCreationFailure)
                    else -> { /* no op */
                    }
                }
            }
        }
    }

    private fun manageDeferredNotifications(siteId: Long) {
        launch {
            if (isRemoteFeatureFlagEnabled(LOCAL_NOTIFICATION_STORE_CREATION_READY)) {
                val name = if (accountStore.account.firstName.isNotNullOrEmpty())
                    accountStore.account.firstName
                else
                    accountStore.account.userName
                localNotificationScheduler.scheduleNotification(StoreCreationCompletedNotification(siteId, name))
            }
            if (isRemoteFeatureFlagEnabled(LOCAL_NOTIFICATION_1D_BEFORE_FREE_TRIAL_EXPIRES)) {
                val in14days = LocalDateTime.now()
                    .plusDays(StoreInstallationViewModel.TRIAL_LENGTH_IN_DAYS)
                    .format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
                localNotificationScheduler.scheduleNotification(
                    FreeTrialExpiringNotification(in14days, siteId)
                )
            }

            if (isRemoteFeatureFlagEnabled(LOCAL_NOTIFICATION_1D_AFTER_FREE_TRIAL_EXPIRES)) {
                val name = if (accountStore.account.firstName.isNotNullOrEmpty())
                    accountStore.account.firstName
                else
                    accountStore.account.userName

                localNotificationScheduler.scheduleNotification(
                    FreeTrialExpiredNotification(name, siteId)
                )
            }
            localNotificationScheduler.scheduleNotification(
                UpgradeToPaidPlanNotification(siteId)
            )
            localNotificationScheduler.scheduleNotification(
                FreeTrialSurveyNotification(siteId)
            )
        }
    }

    object OnCancelPressed : MultiLiveEvent.Event()
    object OnStoreCreationSuccess : MultiLiveEvent.Event()
    object OnStoreCreationFailure : MultiLiveEvent.Event()
}
