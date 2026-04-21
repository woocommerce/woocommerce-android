package com.woocommerce.android.ui.main

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.REVIEW_OPEN
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.FeatureAnnouncement
import com.woocommerce.android.model.Notification
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.notifications.UnseenReviewsCountHandler
import com.woocommerce.android.notifications.WooNotificationType
import com.woocommerce.android.notifications.local.LocalNotificationType
import com.woocommerce.android.notifications.local.LocalNotificationType.BLAZE_ABANDONED_CAMPAIGN_REMINDER
import com.woocommerce.android.notifications.local.LocalNotificationType.BLAZE_NO_CAMPAIGN_REMINDER
import com.woocommerce.android.notifications.local.LocalNotificationType.WOO_POS_SURVEY_CURRENT_USER_REMINDER
import com.woocommerce.android.notifications.local.LocalNotificationType.WOO_POS_SURVEY_POTENTIAL_USER_REMINDER
import com.woocommerce.android.notifications.push.NotificationMessageHandler
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.tools.connectionType
import com.woocommerce.android.ui.ageeligibility.AgeEligibilityChecker
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.main.MainActivityViewModel.MoreMenuBadgeState.Hidden
import com.woocommerce.android.ui.main.MainActivityViewModel.MoreMenuBadgeState.NewFeature
import com.woocommerce.android.ui.main.MainActivityViewModel.MoreMenuBadgeState.UnseenReviews
import com.woocommerce.android.ui.moremenu.MoreMenuNewFeature
import com.woocommerce.android.ui.moremenu.MoreMenuNewFeatureHandler
import com.woocommerce.android.ui.plans.trial.DetermineTrialStatusBarState
import com.woocommerce.android.ui.prefs.PrivacySettingsRepository
import com.woocommerce.android.ui.prefs.RequestedAnalyticsValue
import com.woocommerce.android.ui.shortcuts.AppShortcut
import com.woocommerce.android.ui.whatsnew.FeatureAnnouncementRepository
import com.woocommerce.android.util.BuildConfigWrapper
import com.woocommerce.android.util.SystemVersionUtilsWrapper
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList")
class MainActivityViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val siteStore: SiteStore,
    private val selectedSite: SelectedSite,
    private val notificationHandler: NotificationMessageHandler,
    private val featureAnnouncementRepository: FeatureAnnouncementRepository,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val prefs: AppPrefs,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val resolveAppLink: ResolveAppLink,
    private val privacyRepository: PrivacySettingsRepository,
    private val systemVersionUtilsWrapper: SystemVersionUtilsWrapper,
    ageEligibilityChecker: AgeEligibilityChecker,
    moreMenuNewFeatureHandler: MoreMenuNewFeatureHandler,
    unseenReviewsCountHandler: UnseenReviewsCountHandler,
    determineTrialStatusBarState: DetermineTrialStatusBarState,
) : ScopedViewModel(savedState) {

    val startDestination = if (selectedSite.exists()) R.id.dashboard else R.id.nav_graph_site_picker

    val moreMenuBadgeState = combine(
        unseenReviewsCountHandler.observeUnseenCount(),
        moreMenuNewFeatureHandler.moreMenuNewFeaturesAvailable,
    ) { reviewsCount, features ->
        determineMenuBadgeState(reviewsCount, features)
    }.asLiveData()

    private val _bottomBarState: MutableStateFlow<BottomBarState> = MutableStateFlow(BottomBarState.Visible)
    val bottomBarState = _bottomBarState.asLiveData()

    private val _isNotificationPermissionCardVisible = MutableStateFlow(false)
    val isNotificationsPermissionCardVisible = _isNotificationPermissionCardVisible.asLiveData()

    val trialStatusBarState = determineTrialStatusBarState(_bottomBarState).asLiveData()

    val isUserAgeRangeEligible = ageEligibilityChecker.ageEligibilityState.asLiveData()

    fun handleShortcutAction(action: String?) {
        if (!selectedSite.exists()) return
        when (action) {
            AppShortcut.Payments.action -> {
                analyticsTrackerWrapper.track(
                    AnalyticsEvent.SHORTCUT_PAYMENTS_TAPPED
                )
                triggerEvent(ShortcutOpenPayments)
            }

            AppShortcut.CreateOrder.action -> {
                analyticsTrackerWrapper.track(
                    AnalyticsEvent.SHORTCUT_ORDERS_ADD_NEW
                )
                triggerEvent(ShortcutOpenOrderCreation)
            }
        }
    }

    fun removeOrderNotifications() {
        notificationHandler.removeNotificationsOfTypeFromSystemsBar(
            NotificationChannelType.NEW_ORDER,
            selectedSite.get().siteId
        )
    }

    fun onPushNotificationTapped(localPushId: Int, notification: Notification?) {
        notification?.let {
            // update current selectSite based on the current notification
            val currentSite = selectedSite.get()
            val isSiteSpecificNotification = it.remoteSiteId != 0L &&
                currentSite.connectionType != SiteConnectionType.ApplicationPasswords
            if (isSiteSpecificNotification && it.remoteSiteId != currentSite.siteId) {
                changeSiteAndRestart(it.remoteSiteId, RestartActivityForPushNotification(localPushId, notification))
            } else {
                when (localPushId) {
                    it.getGroupPushId() -> onGroupMessageOpened(it)
                    else -> onSinglePushNotificationOpened(localPushId, it)
                }
            }
        } ?: run {
            triggerEvent(ViewMyStoreStats)
        }
    }

    fun handleIncomingAppLink(uri: Uri?) {
        when (val event = resolveAppLink(uri)) {
            is ResolveAppLink.Action.ChangeSiteAndRestart -> {
                changeSiteAndRestart(event.siteId, RestartActivityForAppLink(event.uri))
            }

            is ResolveAppLink.Action.ViewOrderDetail -> {
                triggerEvent(ViewOrderDetail(event.orderId))
            }

            ResolveAppLink.Action.ViewStats -> {
                triggerEvent(ViewMyStoreStats)
            }

            ResolveAppLink.Action.ViewPayments -> {
                triggerEvent(ViewPayments)
            }

            ResolveAppLink.Action.ViewTapToPay -> {
                triggerEvent(ViewTapToPay)
            }

            ResolveAppLink.Action.ViewWooPosPromo -> {
                triggerEvent(ViewWooPosPromo)
            }

            is ResolveAppLink.Action.ViewUrlInWebView -> {
                triggerEvent(ViewUrlInWebView(event.url))
            }

            ResolveAppLink.Action.DoNothing -> {
                // no-op
            }
        }
    }

    private fun changeSiteAndRestart(remoteSiteId: Long, restartEvent: RestartActivityEvent) {
        // Update selected store
        siteStore.getSiteBySiteId(remoteSiteId)?.let { updatedSite ->
            selectedSite.set(updatedSite)
            triggerEvent(restartEvent)
        } ?: run {
            // If for any reason we can't get the store, show the default screen
            triggerEvent(ViewMyStoreStats)
        }
    }

    private fun onGroupMessageOpened(notification: Notification) {
        notificationHandler.markNotificationsOfTypeTapped(notification.channelType)
        notificationHandler.removeNotificationsOfTypeFromSystemsBar(notification.channelType, notification.remoteSiteId)
        when (notification.channelType) {
            NotificationChannelType.NEW_ORDER -> triggerEvent(ViewOrderList)
            NotificationChannelType.REVIEW -> triggerEvent(ViewReviewList)
            NotificationChannelType.OTHER -> if (notification.isBlazeNotification) {
                triggerEvent(ViewBlazeCampaignList)
            } else {
                triggerEvent(ViewMyStoreStats)
            }
        }
    }

    private fun onSinglePushNotificationOpened(localPushId: Int, notification: Notification) {
        notificationHandler.markNotificationTapped(notification.remoteNoteId)
        notificationHandler.removeTappedNotificationAndSummaryIfNeeded(localPushId, notification)
        when (notification.noteType) {
            is WooNotificationType.NewOrder -> {
                triggerEvent(ViewOrderDetail(notification.uniqueId))
            }

            is WooNotificationType.ProductReview -> {
                analyticsTrackerWrapper.track(REVIEW_OPEN)
                triggerEvent(ViewReviewDetail(notification.uniqueId))
            }

            is WooNotificationType.BlazeStatusUpdate -> triggerEvent(
                ViewBlazeCampaignDetail(campaignId = notification.uniqueId.toString())
            )

            is WooNotificationType.LocalReminder -> error("Local reminder notification should not be handled here")
        }
    }

    private fun determineMenuBadgeState(count: Int, features: List<MoreMenuNewFeature>) =
        if (features.isNotEmpty()) {
            NewFeature
        } else if (count > 0) UnseenReviews(count) else Hidden

    fun showFeatureAnnouncementIfNeeded() {
        launch {
            if (prefs.getLastVersionWithAnnouncement() == buildConfigWrapper.versionName) {
                return@launch
            }

            val announcement = featureAnnouncementRepository.getLatestFeatureAnnouncement(fromCache = false)

            announcement?.let {
                if (announcement.canBeDisplayedOnAppUpgrade(buildConfigWrapper.versionName)) {
                    WooLog.i(T.DEVICE, "Displaying Feature Announcement on main activity")
                    analyticsTrackerWrapper.track(
                        AnalyticsEvent.FEATURE_ANNOUNCEMENT_SHOWN,
                        mapOf(
                            AnalyticsTracker.KEY_ANNOUNCEMENT_VIEW_SOURCE to
                                AnalyticsTracker.VALUE_ANNOUNCEMENT_SOURCE_UPGRADE
                        )
                    )
                    triggerEvent(ShowFeatureAnnouncement(it))
                }
            }
        }
    }

    fun onNotificationOSAlertAllowed() {
        analyticsTrackerWrapper.track(AnalyticsEvent.PUSH_NOTIFICATION_OS_ALERT_ALLOWED)
    }

    fun onNotificationOSAlertDenied() {
        analyticsTrackerWrapper.track(AnalyticsEvent.PUSH_NOTIFICATION_OS_ALERT_DENIED)
    }

    fun checkForNotificationsPermission(hasNotificationsPermission: Boolean) {
        val shouldShowNotificationsPermissionBar = systemVersionUtilsWrapper.isAtLeastT() &&
            !hasNotificationsPermission &&
            !prefs.getWasNotificationsPermissionBarDismissed()

        if (_isNotificationPermissionCardVisible.value != shouldShowNotificationsPermissionBar) {
            _isNotificationPermissionCardVisible.update { shouldShowNotificationsPermissionBar }
            if (shouldShowNotificationsPermissionBar) {
                analyticsTrackerWrapper.track(AnalyticsEvent.NOTIFICATIONS_RATIONALE_SHOWN)
            }
        }
    }

    fun hideBottomNav() {
        _bottomBarState.value = BottomBarState.Hidden
    }

    fun showBottomNav() {
        _bottomBarState.value = BottomBarState.Visible
    }

    fun onNotificationsPermissionBarDismissButtonTapped() {
        analyticsTrackerWrapper.track(AnalyticsEvent.NOTIFICATIONS_RATIONALE_DISMISS_TAPPED)
        prefs.setWasNotificationsPermissionBarDismissed(true)
        _isNotificationPermissionCardVisible.update { false }
    }

    fun onNotificationsPermissionBarAllowButtonTapped() {
        analyticsTrackerWrapper.track(AnalyticsEvent.NOTIFICATIONS_RATIONALE_ALLOW_TAPPED)
        triggerEvent(RequestNotificationsPermission)
    }

    fun onLocalNotificationTapped(notification: Notification) {
        if (notification.remoteSiteId != selectedSite.getOrNull()?.siteId) {
            changeSiteAndRestart(
                notification.remoteSiteId,
                RestartActivityForLocalNotification(notification)
            )
        } else {
            AnalyticsTracker.track(
                AnalyticsEvent.LOCAL_NOTIFICATION_TAPPED,
                mapOf(AnalyticsTracker.KEY_TYPE to notification.tag)
            )
            LocalNotificationType.fromString(notification.tag)?.let {
                when (it) {
                    BLAZE_NO_CAMPAIGN_REMINDER,
                    BLAZE_ABANDONED_CAMPAIGN_REMINDER -> triggerEvent(LaunchBlazeCampaignCreation)

                    WOO_POS_SURVEY_POTENTIAL_USER_REMINDER -> triggerEvent(
                        ViewSurvey(SurveyType.WOO_POS_POTENTIAL_USER)
                    )

                    WOO_POS_SURVEY_CURRENT_USER_REMINDER -> triggerEvent(
                        ViewSurvey(SurveyType.WOO_POS_CURRENT_USER)
                    )
                }
            }
        }
    }

    fun onPrivacyPreferenceUpdateFailed(analyticsEnabled: Boolean) {
        triggerEvent(ShowPrivacyPreferenceUpdatedFailed(analyticsEnabled))
    }

    fun onRequestPrivacyUpdate(analyticsEnabled: Boolean) {
        launch {
            privacyRepository.updateTracksSetting(analyticsEnabled).fold(
                onSuccess = {
                    prefs.savedPrivacySettings = true
                },
                onFailure = {
                    triggerEvent(ShowPrivacyPreferenceUpdatedFailed(analyticsEnabled))
                }
            )
        }
    }

    fun onPrivacySettingsTapped() {
        triggerEvent(ShowPrivacySettings)
    }

    fun onSettingsPrivacyPreferenceUpdateFailed(requestedAnalyticsPreference: RequestedAnalyticsValue) {
        triggerEvent(ShowPrivacySettingsWithError(requestedAnalyticsPreference))
    }

    fun handleIncomingImages(imageUris: List<String>?) {
        if (imageUris.isNullOrEmpty()) return

        analyticsTrackerWrapper.track(AnalyticsEvent.PRODUCT_CREATED_USING_SHARED_IMAGES)

        triggerEvent(CreateNewProductUsingImages(imageUris))
    }

    object ViewOrderList : Event()
    object ViewReviewList : Event()
    object ViewMyStoreStats : Event()
    object ViewPayments : Event()
    object ViewTapToPay : Event()
    object ViewWooPosPromo : Event()
    object RequestNotificationsPermission : Event()
    data class ViewUrlInWebView(
        val url: String,
    ) : Event()
    data class ViewSurvey(val surveyType: SurveyType) : Event()

    object ShortcutOpenPayments : Event()
    object ShortcutOpenOrderCreation : Event()
    object LaunchBlazeCampaignCreation : Event()

    sealed class RestartActivityEvent : Event()
    data class RestartActivityForLocalNotification(val notification: Notification) : RestartActivityEvent()
    data class RestartActivityForPushNotification(val pushId: Int, val notification: Notification) :
        RestartActivityEvent()

    data class RestartActivityForAppLink(val data: Uri) : RestartActivityEvent()

    data class CreateNewProductUsingImages(val imageUris: List<String>) : Event()

    data class ShowFeatureAnnouncement(val announcement: FeatureAnnouncement) : Event()
    data class ViewReviewDetail(val uniqueId: Long) : Event()
    data class ViewOrderDetail(val uniqueId: Long) : Event()
    data class ViewBlazeCampaignDetail(val campaignId: String) : Event()
    object ViewBlazeCampaignList : Event()
    data class ShowPrivacyPreferenceUpdatedFailed(val analyticsEnabled: Boolean) : Event()
    object ShowPrivacySettings : Event()
    data class ShowPrivacySettingsWithError(val requestedAnalyticsValue: RequestedAnalyticsValue) : Event()
    sealed class MoreMenuBadgeState {
        data class UnseenReviews(val count: Int) : MoreMenuBadgeState()
        object NewFeature : MoreMenuBadgeState()
        object Hidden : MoreMenuBadgeState()
    }

    sealed class BottomBarState : Event() {
        object Visible : BottomBarState()
        object Hidden : BottomBarState()
    }
}
