package com.woocommerce.android.notifications.push

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.notifications.NotificationSource
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.tools.connectionType
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
    fun track(stat: AnalyticsEvent, siteId: Long) {
        val site = resolveNotificationSite(siteId) ?: return
        val properties = mutableMapOf<String, Any>().addCommonSiteProperties(site)
        analyticsTrackerWrapper.track(stat, properties)
    }

    fun trackNotificationAnalytics(
        stat: AnalyticsEvent,
        siteId: Long,
        notificationId: String?,
        noteTypeTrackingValue: String,
        source: NotificationSource
    ) {
        val site = resolveNotificationSite(siteId) ?: return
        val properties = mutableMapOf<String, Any>(
            "notification_type" to noteTypeTrackingValue,
            "push_notification_token" to appPrefsWrapper.getFCMToken(),
            "push_notification_source" to source.trackingValue,
            "is_from_selected_site" to site.isSelectedSite()
        ).addCommonSiteProperties(site)
        if (notificationId != null) {
            properties["notification_note_id"] = notificationId
        }
        analyticsTrackerWrapper.track(stat, properties)
    }

    fun trackError(
        stat: AnalyticsEvent,
        siteId: Long,
        errorDescription: String?,
        errorType: String?,
        errorCode: String? = null
    ) {
        val site = resolveNotificationSite(siteId) ?: return
        val properties = mutableMapOf<String, Any>().apply {
            errorDescription?.let { this[AnalyticsTracker.KEY_ERROR_DESC] = it }
            errorType?.let { this[AnalyticsTracker.KEY_ERROR_TYPE] = it }
            errorCode?.let { this[AnalyticsTracker.KEY_ERROR_CODE] = it }
        }.addCommonSiteProperties(site)
        analyticsTrackerWrapper.track(stat, properties)
    }

    // Application-password sites have no wpcom site id, so the payload siteId is unreliable;
    // fall back to the selected site we don't support multi-sites with app passwords.
    private fun resolveNotificationSite(siteId: Long): SiteModel? {
        return if (selectedSite.connectionType == SiteConnectionType.ApplicationPasswords) {
            selectedSite.getOrNull()
        } else {
            siteStore.getSiteBySiteId(siteId)
        }
    }

    private fun SiteModel.isSelectedSite() = if (connectionType == SiteConnectionType.ApplicationPasswords) {
        true
    } else {
        siteId == selectedSite.getOrNull()?.siteId
    }

    private fun MutableMap<String, Any>.addCommonSiteProperties(site: SiteModel) = apply {
        this[AnalyticsTracker.IS_JETPACK_INSTALLED] = site.isJetpackInstalled
        this[AnalyticsTracker.IS_JETPACK_CONNECTED] = site.isJetpackConnected
        this[AnalyticsTracker.IS_JETPACK_CP_CONNECTED] = site.isJetpackCPConnected
        this[AnalyticsTracker.IS_CIAB] = site.isCIABSite()
        site.gardenPartner?.let { this[AnalyticsTracker.GARDEN_PARTNER] = it }
    }

    fun flush() {
        analyticsTrackerWrapper.flush()
    }
}
