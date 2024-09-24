package com.woocommerce.android.ui.main

import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.REVIEW_OPEN
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.FeatureAnnouncement
import com.woocommerce.android.model.FeatureAnnouncementItem
import com.woocommerce.android.model.Notification
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.notifications.UnseenReviewsCountHandler
import com.woocommerce.android.notifications.WooNotificationType
import com.woocommerce.android.notifications.local.LocalNotificationType
import com.woocommerce.android.notifications.local.LocalNotificationType.BLAZE_ABANDONED_CAMPAIGN_REMINDER
import com.woocommerce.android.notifications.local.LocalNotificationType.BLAZE_NO_CAMPAIGN_REMINDER
import com.woocommerce.android.notifications.push.NotificationMessageHandler
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType.Jetpack
import com.woocommerce.android.tools.connectionType
import com.woocommerce.android.ui.main.MainActivityViewModel.MoreMenuBadgeState.Hidden
import com.woocommerce.android.ui.main.MainActivityViewModel.MoreMenuBadgeState.NewFeature
import com.woocommerce.android.ui.main.MainActivityViewModel.MoreMenuBadgeState.UnseenReviews
import com.woocommerce.android.ui.moremenu.MoreMenuNewFeature
import com.woocommerce.android.ui.moremenu.MoreMenuNewFeatureHandler
import com.woocommerce.android.ui.plans.trial.DetermineTrialStatusBarState
import com.woocommerce.android.ui.prefs.PrivacySettingsRepository
import com.woocommerce.android.ui.prefs.RequestedAnalyticsValue
import com.woocommerce.android.ui.whatsnew.FeatureAnnouncementRepository
import com.woocommerce.android.util.BuildConfigWrapper
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.whatsnew.WhatsNewAnnouncementModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.StringUtils
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
    moreMenuNewFeatureHandler: MoreMenuNewFeatureHandler,
    unseenReviewsCountHandler: UnseenReviewsCountHandler,
    determineTrialStatusBarState: DetermineTrialStatusBarState,
) : ScopedViewModel(savedState) {
    init {
        launch {
            featureAnnouncementRepository.getFeatureAnnouncements(fromCache = false)
        }
    }

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

    fun handleShortcutAction(action: String?) {
        if (!selectedSite.exists()) return
        when (action) {
            SHORTCUT_PAYMENTS -> {
                analyticsTrackerWrapper.track(
                    AnalyticsEvent.SHORTCUT_PAYMENTS_TAPPED
                )
                triggerEvent(ShortcutOpenPayments)
            }

            SHORTCUT_OPEN_ORDER_CREATION -> {
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
            val isSiteSpecificNotification = it.remoteSiteId != 0L
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
                triggerEvent(ViewOrderDetail(uniqueId = event.orderId, remoteNoteId = 0L))
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
        notificationHandler.removeNotificationByNotificationIdFromSystemsBar(localPushId)
        when (notification.noteType) {
            is WooNotificationType.NewOrder -> {
                when {
                    siteStore.getSiteBySiteId(notification.remoteSiteId) != null -> triggerEvent(
                        ViewOrderDetail(
                            notification.uniqueId,
                            notification.remoteNoteId
                        )
                    )

                    else -> triggerEvent(ViewOrderList)
                }
            }

            is WooNotificationType.ProductReview -> {
                analyticsTrackerWrapper.track(REVIEW_OPEN)
                triggerEvent(ViewReviewDetail(notification.uniqueId))
            }

            is WooNotificationType.BlazeStatusUpdate -> triggerEvent(
                ViewBlazeCampaignDetail(
                    campaignId = notification.uniqueId.toString(),
                    isOpenedFromPush = true
                )
            )

            is WooNotificationType.LocalReminder -> error("Local reminder notification should not be handled here")
        }
    }

    private fun determineMenuBadgeState(count: Int, features: List<MoreMenuNewFeature>) =
        if (features.isNotEmpty()) {
            NewFeature
        } else if (count > 0) UnseenReviews(count) else Hidden

    fun showFeatureAnnouncementIfNeeded() {
        triggerEvent(
            ShowFeatureAnnouncement(
                WhatsNewAnnouncementModel(
                    appVersionName = "1.0",
                    announcementVersion = 1,
                    minimumAppVersion = "1.0",
                    maximumAppVersion = "1.0",
                    appVersionTargets = listOf("1.0"),
                    detailsUrl = "https://woocommerce.com",
                    isLocalized = true,
                    features = listOf(
                        WhatsNewAnnouncementModel.WhatsNewAnnouncementFeature(
                            title = "Title",
                            subtitle = "Subtitle Subtitle Subtitle Subtitle Subtitle Subtitle Subtitle Subtitle Subtitle Subtitle Subtitle  Subtitle",
                            iconBase64 = "IconBase64",
                            iconUrl = "IconUrl"
                        )
                    ),
                    responseLocale = "en"
                ).build()
            )
        )
//        launch {
//            val cachedAnnouncement = featureAnnouncementRepository.getLatestFeatureAnnouncement(fromCache = true)
//
//            // Feature Announcement dialog can be shown on app resume, if these criteria are filled:
//            // 1. Current version is different from the last version where announcement was shown
//            // 2. Announcement content is valid and can be displayed
//            cachedAnnouncement?.let {
//                if (prefs.getLastVersionWithAnnouncement() != buildConfigWrapper.versionName &&
//                    cachedAnnouncement.canBeDisplayedOnAppUpgrade(buildConfigWrapper.versionName)
//                ) {
//                    WooLog.i(T.DEVICE, "Displaying Feature Announcement on main activity")
//                    analyticsTrackerWrapper.track(
//                        AnalyticsEvent.FEATURE_ANNOUNCEMENT_SHOWN,
//                        mapOf(
//                            AnalyticsTracker.KEY_ANNOUNCEMENT_VIEW_SOURCE to
//                                AnalyticsTracker.VALUE_ANNOUNCEMENT_SOURCE_UPGRADE
//                        )
//                    )
//
//                }
//            }
//        }
    }

    fun WhatsNewAnnouncementModel.build(): FeatureAnnouncement {
        return FeatureAnnouncement(
            appVersionName,
            announcementVersion,
            minimumAppVersion,
            maximumAppVersion,
            appVersionTargets,
            detailsUrl,
            isLocalized,
            features.map {
                it.build()
            }
        )
    }

    fun WhatsNewAnnouncementModel.WhatsNewAnnouncementFeature.build(): FeatureAnnouncementItem {
        return FeatureAnnouncementItem(
            StringUtils.notNullStr(title),
            StringUtils.notNullStr(subtitle),
            StringUtils.notNullStr(iconBase64),
            StringUtils.notNullStr(iconUrl)
        )
    }

    fun checkForNotificationsPermission(hasNotificationsPermission: Boolean) {
        val shouldShowNotificationsPermissionBar = VERSION.SDK_INT >= VERSION_CODES.TIRAMISU &&
            !hasNotificationsPermission && !AppPrefs.getWasNotificationsPermissionBarDismissed() &&
            selectedSite.get().connectionType == Jetpack

        _isNotificationPermissionCardVisible.update { shouldShowNotificationsPermissionBar }
    }

    fun hideBottomNav() {
        _bottomBarState.value = BottomBarState.Hidden
    }

    fun showBottomNav() {
        _bottomBarState.value = BottomBarState.Visible
    }

    fun onNotificationsPermissionBarDismissButtonTapped() {
        AppPrefs.setWasNotificationsPermissionBarDismissed(true)
        _isNotificationPermissionCardVisible.update { false }
    }

    fun onNotificationsPermissionBarAllowButtonTapped() {
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
    object RequestNotificationsPermission : Event()
    data class ViewUrlInWebView(
        val url: String,
    ) : Event()

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
    data class ViewOrderDetail(val uniqueId: Long, val remoteNoteId: Long) : Event()
    data class ViewBlazeCampaignDetail(val campaignId: String, val isOpenedFromPush: Boolean) : Event()
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

    companion object {
        const val SHORTCUT_PAYMENTS = "com.woocommerce.android.payments"
        const val SHORTCUT_OPEN_ORDER_CREATION = "com.woocommerce.android.ordercreation"
    }
}
