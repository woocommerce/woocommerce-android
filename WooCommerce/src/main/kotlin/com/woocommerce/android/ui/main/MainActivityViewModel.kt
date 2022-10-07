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
import com.woocommerce.android.push.NotificationChannelType
import com.woocommerce.android.push.NotificationMessageHandler
import com.woocommerce.android.push.UnseenReviewsCountHandler
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.main.MainActivityViewModel.MoreMenuBadgeState.Hidden
import com.woocommerce.android.ui.main.MainActivityViewModel.MoreMenuBadgeState.NewFeature
import com.woocommerce.android.ui.main.MainActivityViewModel.MoreMenuBadgeState.UnseenReviews
import com.woocommerce.android.ui.moremenu.MoreMenuNewFeature
import com.woocommerce.android.ui.moremenu.MoreMenuNewFeatureHandler
import com.woocommerce.android.ui.whatsnew.FeatureAnnouncementRepository
import com.woocommerce.android.util.BuildConfigWrapper
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

@HiltViewModel
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
    moreMenuNewFeatureHandler: MoreMenuNewFeatureHandler,
    unseenReviewsCountHandler: UnseenReviewsCountHandler,
) : ScopedViewModel(savedState) {
    init {
        launch {
            featureAnnouncementRepository.getFeatureAnnouncements(fromCache = false)
        }
    }

    val startDestination = if (selectedSite.exists()) R.id.dashboard else R.id.sitePickerFragment

    val moreMenuBadgeState = combine(
        unseenReviewsCountHandler.observeUnseenCount(),
        moreMenuNewFeatureHandler.moreMenuNewFeaturesAvailable,
    ) { reviewsCount, features ->
        determineMenuBadgeState(reviewsCount, features)
    }.asLiveData()

    fun removeOrderNotifications() {
        notificationHandler.removeNotificationsOfTypeFromSystemsBar(
            NotificationChannelType.NEW_ORDER, selectedSite.get().siteId
        )
    }

    fun handleIncomingNotification(localPushId: Int, notification: Notification?) {
        notification?.let {
            // update current selectSite based on the current notification
            val currentSite = selectedSite.get()
            val isSiteSpecificNotification = it.remoteSiteId != 0L
            if (isSiteSpecificNotification && it.remoteSiteId != currentSite.siteId) {
                changeSiteAndRestart(it.remoteSiteId, RestartActivityForNotification(localPushId, notification))
            } else {
                when (localPushId) {
                    it.getGroupPushId() -> onGroupMessageOpened(it.channelType, it.remoteSiteId)
                    it.noteId -> onZendeskNotificationOpened(localPushId, it.noteId.toLong())
                    else -> onSingleNotificationOpened(localPushId, it)
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
            ResolveAppLink.Action.DoNothing -> {
                // no-op
            }
        }
    }

    private fun changeSiteAndRestart(remoteSiteId: Long, restartEvent: Event) {
        // Update selected store
        siteStore.getSiteBySiteId(remoteSiteId)?.let { updatedSite ->
            selectedSite.set(updatedSite)
            // Recreate activity before showing notification
            triggerEvent(restartEvent)
        } ?: run {
            // If for any reason we can't get the store, show the default screen
            triggerEvent(ViewMyStoreStats)
        }
    }

    private fun onGroupMessageOpened(notificationChannelType: NotificationChannelType, remoteSiteId: Long) {
        notificationHandler.markNotificationsOfTypeTapped(notificationChannelType)
        notificationHandler.removeNotificationsOfTypeFromSystemsBar(notificationChannelType, remoteSiteId)
        when (notificationChannelType) {
            NotificationChannelType.NEW_ORDER -> triggerEvent(ViewOrderList)
            NotificationChannelType.REVIEW -> triggerEvent(ViewReviewList)
            else -> triggerEvent(ViewMyStoreStats)
        }
    }

    private fun onZendeskNotificationOpened(localPushId: Int, remoteNoteId: Long) {
        notificationHandler.markNotificationTapped(remoteNoteId)
        notificationHandler.removeNotificationByPushIdFromSystemsBar(localPushId)
        triggerEvent(ViewZendeskTickets)
    }

    private fun onSingleNotificationOpened(localPushId: Int, notification: Notification) {
        notificationHandler.markNotificationTapped(notification.remoteNoteId)
        notificationHandler.removeNotificationByPushIdFromSystemsBar(localPushId)
        if (notification.channelType == NotificationChannelType.REVIEW) {
            analyticsTrackerWrapper.track(REVIEW_OPEN)
            triggerEvent(ViewReviewDetail(notification.uniqueId))
        } else if (notification.channelType == NotificationChannelType.NEW_ORDER) {
            if (siteStore.getSiteBySiteId(notification.remoteSiteId) != null) {
                triggerEvent(ViewOrderDetail(notification.uniqueId, notification.remoteNoteId))
            } else {
                // the site does not exist locally, open order list
                triggerEvent(ViewOrderList)
            }
        }
    }

    private fun determineMenuBadgeState(count: Int, features: List<MoreMenuNewFeature>) =
        if (features.isNotEmpty()) NewFeature
        else if (count > 0) UnseenReviews(count) else Hidden

    fun showFeatureAnnouncementIfNeeded() {
        launch {
            val cachedAnnouncement = featureAnnouncementRepository.getLatestFeatureAnnouncement(fromCache = true)

            // Feature Announcement dialog can be shown on app resume, if these criteria are filled:
            // 1. Current version is different from the last version where announcement was shown
            // 2. Announcement content is valid and can be displayed
            cachedAnnouncement?.let {
                if (prefs.getLastVersionWithAnnouncement() != buildConfigWrapper.versionName &&
                    cachedAnnouncement.canBeDisplayedOnAppUpgrade(buildConfigWrapper.versionName)
                ) {
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

    object ViewOrderList : Event()
    object ViewReviewList : Event()
    object ViewMyStoreStats : Event()
    object ViewZendeskTickets : Event()
    object ViewPayments : Event()
    data class RestartActivityForNotification(val pushId: Int, val notification: Notification) : Event()
    data class RestartActivityForAppLink(val data: Uri) : Event()
    data class ShowFeatureAnnouncement(val announcement: FeatureAnnouncement) : Event()
    data class ViewReviewDetail(val uniqueId: Long) : Event()
    data class ViewOrderDetail(val uniqueId: Long, val remoteNoteId: Long) : Event()

    sealed class MoreMenuBadgeState {
        data class UnseenReviews(val count: Int) : MoreMenuBadgeState()
        object NewFeature : MoreMenuBadgeState()
        object Hidden : MoreMenuBadgeState()
    }
}
