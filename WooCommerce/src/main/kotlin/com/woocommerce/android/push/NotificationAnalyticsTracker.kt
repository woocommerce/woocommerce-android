package com.woocommerce.android.push

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
        val isFromSelectedSite = selectedSite.getIfExists()?.siteId == notification.remoteSiteId
        val properties = mutableMapOf<String, Any>()
        properties["notification_note_id"] = notification.remoteNoteId
        properties["notification_type"] = notification.noteType.name
        properties["push_notification_token"] = appPrefsWrapper.getFCMToken()
        properties["is_from_selected_site"] = isFromSelectedSite == true
        analyticsTrackerWrapper.track(stat, properties)
    }

    fun flush() {
        analyticsTrackerWrapper.flush()
    }
}
