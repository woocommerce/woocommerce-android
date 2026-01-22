package com.woocommerce.android.notifications.push

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.isCIABSite
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationAnalyticsTracker @Inject constructor(
    private val siteStore: SiteStore,
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
        siteId: Long,
        remoteNoteId: Long,
        noteTypeTrackingValue: String
    ) {
        val site = siteStore.getSiteBySiteId(siteId) ?: return
        val isFromSelectedSite = selectedSite.getIfExists()?.siteId == siteId
        val properties = mutableMapOf<String, Any>(
            "notification_note_id" to remoteNoteId,
            "notification_type" to noteTypeTrackingValue,
            "push_notification_token" to appPrefsWrapper.getFCMToken(),
            "is_from_selected_site" to isFromSelectedSite
        ).addCommonSiteProperties(site)
        analyticsTrackerWrapper.track(stat, properties)
    }

    private fun MutableMap<String, Any>.addCommonSiteProperties(site: SiteModel) = apply {
        this["is_jetpack_installed"] = site.isJetpackInstalled
        this["is_jetpack_connected"] = site.isJetpackConnected
        this["is_jetpack_cp_connected"] = site.isJetpackCPConnected
        this["is_ciab"] = site.isCIABSite()
        site.gardenPartner?.let { this["garden_partner"] = it }
    }

    fun flush() {
        analyticsTrackerWrapper.flush()
    }
}
