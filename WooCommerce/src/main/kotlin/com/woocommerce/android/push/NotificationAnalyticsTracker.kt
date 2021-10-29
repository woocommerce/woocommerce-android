package com.woocommerce.android.push

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Notification
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.PreferencesWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationAnalyticsTracker @Inject constructor(
    private val selectedSite: SelectedSite,
    private val preferencesWrapper: PreferencesWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun trackNotificationAnalytics(stat: AnalyticsTracker.Stat, notification: Notification) {
        val isFromSelectedSite = selectedSite.getIfExists()?.siteId == notification.remoteSiteId
        val properties = mutableMapOf<String, Any>()
        properties["notification_note_id"] = notification.remoteNoteId
        properties["notification_type"] = notification.noteType.name
        properties["push_notification_token"] = preferencesWrapper.getFCMToken() ?: ""
        properties["is_from_selected_site"] = isFromSelectedSite == true
        analyticsTrackerWrapper.track(stat, properties)
    }

    fun flush() {
        analyticsTrackerWrapper.flush()
    }
}
