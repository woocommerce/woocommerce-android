package com.woocommerce.android.notifications.push

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Notification
import com.woocommerce.android.tools.SelectedSite
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationAnalyticsTracker @Inject constructor(
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun trackNotificationAnalytics(stat: AnalyticsEvent, notification: Notification) {
        trackNotificationAnalytics(
            stat = stat,
            remoteNoteId = notification.remoteNoteId,
            remoteSiteId = notification.remoteSiteId,
            noteTypeTrackingValue = notification.noteType.trackingValue
        )
    }

    fun trackNotificationAnalytics(
        stat: AnalyticsEvent,
        remoteNoteId: Long,
        remoteSiteId: Long,
        noteTypeTrackingValue: String
    ) {
        val isFromSelectedSite = selectedSite.getIfExists()?.siteId == remoteSiteId
        val properties = mutableMapOf<String, Any>()
        properties["notification_note_id"] = remoteNoteId
        properties["notification_type"] = noteTypeTrackingValue
        properties["push_notification_token"] = appPrefsWrapper.getFCMToken()
        properties["is_from_selected_site"] = isFromSelectedSite
        analyticsTrackerWrapper.track(stat, properties)
    }

    fun flush() {
        analyticsTrackerWrapper.flush()
    }
}
