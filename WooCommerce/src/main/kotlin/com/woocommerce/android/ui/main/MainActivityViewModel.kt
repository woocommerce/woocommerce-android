package com.woocommerce.android.ui.main

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Notification
import com.woocommerce.android.push.NotificationChannelType
import com.woocommerce.android.push.NotificationMessageHandler
import com.woocommerce.android.push.getGroupId
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val siteStore: SiteStore,
    private val notificationHandler: NotificationMessageHandler
) : ScopedViewModel(savedState) {
    fun removeReviewNotifications() {
        notificationHandler.removeNotificationsOfTypeFromSystemsBar(NotificationChannelType.REVIEW)
    }

    fun removeOrderNotifications() {
        notificationHandler.removeNotificationsOfTypeFromSystemsBar(NotificationChannelType.NEW_ORDER)
    }

    fun handleIncomingNotification(localPushId: Int, notification: Notification?) {
        notification?.let {
            when (localPushId) {
                it.channelType.getGroupId() -> onGroupMessageOpened(it.channelType)
                it.noteId -> onZendeskNotificationOpened(localPushId, it.noteId.toLong())
                else -> onSingleNotificationOpened(localPushId, it)
            }
        } ?: run {
            triggerEvent(ViewMyStoreStats)
        }
    }

    private fun onGroupMessageOpened(notificationChannelType: NotificationChannelType) {
        notificationHandler.markNotificationsOfTypeTapped(notificationChannelType)
        notificationHandler.removeNotificationsOfTypeFromSystemsBar(notificationChannelType)
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
            triggerEvent(ViewReviewDetail(notification.uniqueId))
        } else if (notification.channelType == NotificationChannelType.NEW_ORDER) {
            siteStore.getSiteBySiteId(notification.remoteSiteId)?.let { siteModel ->
                triggerEvent(ViewOrderDetail(notification.uniqueId, siteModel.id, notification.remoteNoteId))
            } ?: run {
                // the site does not exist locally, open order list
                triggerEvent(ViewOrderList)
            }
        }
    }

    object ViewOrderList : Event()
    object ViewReviewList : Event()
    object ViewMyStoreStats : Event()
    object ViewZendeskTickets : Event()
    data class ViewReviewDetail(val uniqueId: Long) : Event()
    data class ViewOrderDetail(val uniqueId: Long, val localSiteId: Int, val remoteNoteId: Long) : Event()
}
