package com.woocommerce.android.notifications.push

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.notifications.NotificationSource
import com.woocommerce.android.tools.ResolveSiteBySiteId
import com.woocommerce.android.tools.SelectedSite
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

class NotificationAnalyticsTrackerTest {
    private val site = createWpComSite(SITE_ID, SITE_URL)
    private val selectedSiteModel = createWpComSite(SELECTED_SITE_ID, SELECTED_SITE_URL)
    private val resolveSiteBySiteId: ResolveSiteBySiteId = mock {
        on { invoke(SITE_ID) } doReturn site
    }
    private val selectedSite: SelectedSite = mock {
        on { getOrNull() } doReturn selectedSiteModel
    }
    private val appPrefsWrapper: AppPrefsWrapper = mock {
        on { getFCMToken() } doReturn "fcm-token"
    }
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    private val tracker = NotificationAnalyticsTracker(
        resolveSiteBySiteId = resolveSiteBySiteId,
        selectedSite = selectedSite,
        appPrefsWrapper = appPrefsWrapper,
        analyticsTrackerWrapper = analyticsTrackerWrapper
    )

    @Test
    fun `given wpcom notification id, when tracking, then include it verbatim`() {
        tracker.trackNotificationAnalytics(
            stat = AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED,
            siteId = SITE_ID,
            notificationId = "987",
            noteTypeTrackingValue = "store_order",
            source = NotificationSource.WPCOM
        )

        val properties = capturedProperties(AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED)
        assertThat(properties).containsEntry("notification_note_id", "987")
        assertThat(properties).containsEntry("push_notification_source", "wpcom")
    }

    @Test
    fun `given woo-driven composite notification id, when tracking, then include composite string`() {
        tracker.trackNotificationAnalytics(
            stat = AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED,
            siteId = SITE_ID,
            notificationId = "12345:order:4321",
            noteTypeTrackingValue = "new_order",
            source = NotificationSource.WOO_DRIVEN
        )

        val properties = capturedProperties(AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED)
        assertThat(properties).containsEntry("notification_note_id", "12345:order:4321")
        assertThat(properties).containsEntry("push_notification_source", "woo_driven")
    }

    @Test
    fun `given notification id is null, when tracking, then omit note id property`() {
        tracker.trackNotificationAnalytics(
            stat = AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED,
            siteId = SITE_ID,
            notificationId = null,
            noteTypeTrackingValue = "new_order",
            source = NotificationSource.WOO_DRIVEN
        )

        val properties = capturedProperties(AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED)
        assertThat(properties).doesNotContainKey("notification_note_id")
        assertThat(properties).containsEntry("notification_type", "new_order")
        assertThat(properties).containsEntry("push_notification_source", "woo_driven")
    }

    @Test
    fun `given known origin differs from selected site, when tracking push events, then include origin properties`() {
        listOf(AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED, AnalyticsEvent.PUSH_NOTIFICATION_TAPPED).forEach { stat ->
            tracker.trackNotificationAnalytics(stat, SITE_ID, "987", "store_order", NotificationSource.WPCOM)

            val properties = capturedProperties(stat)
            assertOriginProperties(properties)
            assertThat(properties).containsEntry("is_from_selected_site", false)
        }
    }

    @Test
    fun `given site does not exist, when tracking notification, then do not track`() {
        tracker.trackNotificationAnalytics(
            stat = AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED,
            siteId = 99999L,
            notificationId = "987",
            noteTypeTrackingValue = "store_order",
            source = NotificationSource.WPCOM
        )

        verify(analyticsTrackerWrapper, never()).track(any(), any<Map<String, Any>>())
    }

    @Test
    fun `given app-password site, when tracking, then use resolved selected site properties`() {
        val appPasswordSite = SiteModel().apply {
            siteId = 0
            url = APP_PASSWORD_SITE_URL
            origin = SiteModel.ORIGIN_WPAPI
        }
        whenever(resolveSiteBySiteId(PAYLOAD_SITE_ID)).thenReturn(appPasswordSite)
        whenever(selectedSite.getOrNull()).thenReturn(appPasswordSite)

        tracker.trackNotificationAnalytics(
            stat = AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED,
            siteId = PAYLOAD_SITE_ID,
            notificationId = "7777:order:42",
            noteTypeTrackingValue = "new_order",
            source = NotificationSource.WOO_DRIVEN
        )

        val properties = capturedProperties(AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED)
        assertThat(properties).containsEntry("is_from_selected_site", true)
        assertThat(properties).containsEntry(AnalyticsTracker.KEY_BLOG_ID, 0L)
        assertThat(properties).containsEntry(AnalyticsTracker.KEY_SITE_URL, APP_PASSWORD_SITE_URL)
        assertThat(properties).containsEntry("notification_note_id", "7777:order:42")
    }

    @Test
    fun `given known origin, when tracking token results, then include origin properties`() {
        tracker.track(AnalyticsEvent.WOO_PUSH_TOKEN_REGISTER_SUCCESS, SITE_ID)
        tracker.trackError(
            AnalyticsEvent.WOO_PUSH_TOKEN_REGISTER_ERROR,
            SITE_ID,
            "failed",
            "network"
        )

        assertOriginProperties(capturedProperties(AnalyticsEvent.WOO_PUSH_TOKEN_REGISTER_SUCCESS))
        val errorProperties = capturedProperties(AnalyticsEvent.WOO_PUSH_TOKEN_REGISTER_ERROR)
        assertOriginProperties(errorProperties)
        assertThat(errorProperties).containsEntry(AnalyticsTracker.KEY_ERROR_DESC, "failed")
        assertThat(errorProperties).containsEntry(AnalyticsTracker.KEY_ERROR_TYPE, "network")
    }

    private fun capturedProperties(stat: AnalyticsEvent): Map<String, Any> {
        val captor = argumentCaptor<Map<String, Any>>()
        verify(analyticsTrackerWrapper).track(eq(stat), captor.capture())
        return captor.firstValue
    }

    private fun assertOriginProperties(properties: Map<String, Any>) {
        assertThat(properties).containsEntry(AnalyticsTracker.KEY_BLOG_ID, SITE_ID)
        assertThat(properties).containsEntry(AnalyticsTracker.KEY_SITE_URL, SITE_URL)
        assertThat(properties).containsEntry(AnalyticsTracker.IS_JETPACK_INSTALLED, true)
        assertThat(properties).containsEntry(AnalyticsTracker.IS_JETPACK_CONNECTED, true)
        assertThat(properties).containsEntry(AnalyticsTracker.IS_JETPACK_CP_CONNECTED, false)
    }

    private companion object {
        const val SITE_ID = 12345L
        const val SITE_URL = "https://origin.example.com"
        const val SELECTED_SITE_ID = 54321L
        const val SELECTED_SITE_URL = "https://selected.example.com"
        const val PAYLOAD_SITE_ID = 7777L
        const val APP_PASSWORD_SITE_URL = "https://app-password.example.com"

        fun createWpComSite(siteId: Long, url: String) = SiteModel().apply {
            this.siteId = siteId
            this.url = url
            origin = SiteModel.ORIGIN_WPCOM_REST
            setIsJetpackInstalled(true)
            setIsJetpackConnected(true)
            setIsJetpackCPConnected(false)
        }
    }
}
