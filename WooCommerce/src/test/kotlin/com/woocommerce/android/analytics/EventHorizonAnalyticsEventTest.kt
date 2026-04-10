package com.woocommerce.android.analytics

import com.automattic.eventhorizon.Trackable
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class EventHorizonAnalyticsEventTest {
    @Test
    fun `when created from Trackable, then name matches analyticsName`() {
        val trackable = mock<Trackable>()
        whenever(trackable.analyticsName).thenReturn("booking_list_view")

        val event = EventHorizonAnalyticsEvent(trackable)

        assertThat(event.name).isEqualTo("booking_list_view")
    }

    @Test
    fun `when created from Trackable, then siteless is false`() {
        val trackable = mock<Trackable>()
        whenever(trackable.analyticsName).thenReturn("booking_list_view")

        val event = EventHorizonAnalyticsEvent(trackable)

        assertThat(event.siteless).isFalse()
    }

    @Test
    fun `when created from Trackable, then isPosEvent is false`() {
        val trackable = mock<Trackable>()
        whenever(trackable.analyticsName).thenReturn("booking_list_view")

        val event = EventHorizonAnalyticsEvent(trackable)

        assertThat(event.isPosEvent).isFalse()
    }
}
