package com.woocommerce.android.ui.main

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Notification
import com.woocommerce.android.push.NotificationChannelType
import com.woocommerce.android.push.NotificationMessageHandler
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val siteStore: SiteStore,
    private val selectedSite: SelectedSite,
    private val notificationHandler: NotificationMessageHandler
) : ScopedViewModel(savedState) {
    fun removeReviewNotifications() {
        notificationHandler.removeNotificationsOfTypeFromSystemsBar(
            NotificationChannelType.REVIEW, selectedSite.get().siteId
        )
    }

    fun removeOrderNotifications() {
        notificationHandler.removeNotificationsOfTypeFromSystemsBar(
            NotificationChannelType.NEW_ORDER, selectedSite.get().siteId
        )
    }

    /**
     * Checks the active store against the incoming notification, then navigates to the correct screen
     *
     * @return true if the notification has been handled, false otherwise
     */
    fun handleIncomingNotification(localPushId: Int, notification: Notification?): Boolean {
        return notification?.let {
            // update current selectSite based on the current notification
            val currentSite = selectedSite.get()
            if (it.remoteSiteId != currentSite.siteId) {
                // Update selected store
                siteStore.getSiteBySiteId(it.remoteSiteId)?.let { updatedSite ->
                    selectedSite.set(updatedSite)
                }

                // Recreate activity
                // Post the event to the main thread, to give the consumer a chance to handle the result first
                launch(Dispatchers.Main) {
                    triggerEvent(RecreateActivity)
                }
                false
            } else {
                when (localPushId) {
                    it.getGroupPushId() -> onGroupMessageOpened(it.channelType, it.remoteSiteId)
                    it.noteId -> onZendeskNotificationOpened(localPushId, it.noteId.toLong())
                    else -> onSingleNotificationOpened(localPushId, it)
                }
                true
            }
        } ?: run {
            triggerEvent(ViewMyStoreStats)
            true
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
    object RecreateActivity : Event()
    data class ViewReviewDetail(val uniqueId: Long) : Event()
    data class ViewOrderDetail(val uniqueId: Long, val localSiteId: Int, val remoteNoteId: Long) : Event()
}
