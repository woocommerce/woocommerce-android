package com.woocommerce.android.notifications.push

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
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
    fun `given remote note id is non zero, when tracking notification, then include note id property`() {
        tracker.trackNotificationAnalytics(
            stat = AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED,
            siteId = siteId,
            remoteNoteId = 987L,
            noteTypeTrackingValue = "store_order"
        )

        val captor = argumentCaptor<Map<String, Any>>()
        verify(analyticsTrackerWrapper).track(eq(AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED), captor.capture())
        assertThat(captor.firstValue).containsEntry("notification_note_id", 987L)
    }

    @Test
    fun `given remote note id is zero, when tracking notification, then omit note id property`() {
        tracker.trackNotificationAnalytics(
            stat = AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED,
            siteId = siteId,
            remoteNoteId = 0L,
            noteTypeTrackingValue = "store_order"
        )

        val captor = argumentCaptor<Map<String, Any>>()
        verify(analyticsTrackerWrapper).track(eq(AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED), captor.capture())
        assertThat(captor.firstValue).doesNotContainKey("notification_note_id")
        assertThat(captor.firstValue).containsEntry("notification_type", "store_order")
    }

    @Test
    fun `given site does not exist, when tracking notification, then do not track`() {
        tracker.trackNotificationAnalytics(
            stat = AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED,
            siteId = 99999L,
            remoteNoteId = 987L,
            noteTypeTrackingValue = "store_order"
        )

        verify(analyticsTrackerWrapper, never()).track(any(), any<Map<String, Any>>())
    }
}
