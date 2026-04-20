package com.woocommerce.android.notifications.push

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.notifications.NotificationSource
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
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore

class NotificationAnalyticsTrackerTest {
    private val siteId = 12345L
    private val site: SiteModel = mock()
    private val siteStore: SiteStore = mock {
        on { getSiteBySiteId(siteId) } doReturn site
    }
    private val selectedSite: SelectedSite = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock {
        on { getFCMToken() } doReturn "fcm-token"
    }
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    private val tracker = NotificationAnalyticsTracker(
        siteStore = siteStore,
        selectedSite = selectedSite,
        appPrefsWrapper = appPrefsWrapper,
        analyticsTrackerWrapper = analyticsTrackerWrapper
    )

    @Test
    fun `given wpcom notification id, when tracking, then include it verbatim`() {
        tracker.trackNotificationAnalytics(
            stat = AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED,
            siteId = siteId,
            notificationId = "987",
            noteTypeTrackingValue = "store_order",
            source = NotificationSource.WPCOM
        )

        val captor = argumentCaptor<Map<String, Any>>()
        verify(analyticsTrackerWrapper).track(eq(AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED), captor.capture())
        assertThat(captor.firstValue).containsEntry("notification_note_id", "987")
        assertThat(captor.firstValue).containsEntry("push_notification_source", "wpcom")
    }

    @Test
    fun `given woo-driven composite notification id, when tracking, then include composite string`() {
        tracker.trackNotificationAnalytics(
            stat = AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED,
            siteId = siteId,
            notificationId = "12345:order:4321",
            noteTypeTrackingValue = "new_order",
            source = NotificationSource.WOO_DRIVEN
        )

        val captor = argumentCaptor<Map<String, Any>>()
        verify(analyticsTrackerWrapper).track(eq(AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED), captor.capture())
        assertThat(captor.firstValue).containsEntry("notification_note_id", "12345:order:4321")
        assertThat(captor.firstValue).containsEntry("push_notification_source", "woo_driven")
    }

    @Test
    fun `given notification id is null, when tracking, then omit note id property`() {
        tracker.trackNotificationAnalytics(
            stat = AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED,
            siteId = siteId,
            notificationId = null,
            noteTypeTrackingValue = "new_order",
            source = NotificationSource.WOO_DRIVEN
        )

        val captor = argumentCaptor<Map<String, Any>>()
        verify(analyticsTrackerWrapper).track(eq(AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED), captor.capture())
        assertThat(captor.firstValue).doesNotContainKey("notification_note_id")
        assertThat(captor.firstValue).containsEntry("notification_type", "new_order")
        assertThat(captor.firstValue).containsEntry("push_notification_source", "woo_driven")
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
}
