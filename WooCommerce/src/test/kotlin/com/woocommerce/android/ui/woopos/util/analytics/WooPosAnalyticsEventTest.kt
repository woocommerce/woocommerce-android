package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class WooPosAnalyticsEventTest {
    @Test
    fun `given every non-launchability reason, when tracked, then each maps to a distinct value`() {
        // GIVEN
        val reasons = WooPosLaunchability.NonLaunchabilityReason.entries

        // WHEN
        val trackedReasons = reasons.map { WooPosAnalyticsEvent.Event.IneligibleUIShown(it).properties["reason"] }

        // THEN
        assertThat(trackedReasons).doesNotContainNull()
        assertThat(trackedReasons).doesNotHaveDuplicates()
        assertThat(trackedReasons).doesNotContain("other")
    }
}
