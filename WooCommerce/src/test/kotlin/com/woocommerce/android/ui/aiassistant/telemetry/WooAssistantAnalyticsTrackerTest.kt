package com.woocommerce.android.ui.aiassistant.telemetry

import com.automattic.eventhorizon.Trackable
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WooAssistantAnalyticsTrackerTest : BaseUnitTest() {
    @Test
    fun `when trackable is tracked, then wrapper receives the same instance`() {
        val tracker = mock<AnalyticsTrackerWrapper>()
        val trackable = mock<Trackable>()

        WooAssistantAnalyticsTracker(tracker).track(trackable)

        verify(tracker).track(trackable)
    }
}
