package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.RefundFlow
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

    @Test
    fun `given a refund failure without an error code, when tracked, then the code property is omitted`() {
        // GIVEN
        val event = WooPosAnalyticsEvent.Event.RefundProcessingFailed(
            refundFlow = RefundFlow.LOCAL,
            apiErrorCode = null,
        )

        // THEN iOS omits the property too, so both platforms answer "no code" the same way
        assertThat(event.properties).doesNotContainKey("api_error_code")
        assertThat(event.properties).containsEntry(RefundFlow.REFUND_FLOW, "local")
    }

    @Test
    fun `given a refund failure with an error code, when tracked, then the code property carries it`() {
        // GIVEN
        val event = WooPosAnalyticsEvent.Event.RefundProcessingFailed(
            refundFlow = RefundFlow.SERVER_COMPUTED,
            apiErrorCode = "woocommerce_rest_gateway_refund_rejected",
        )

        // THEN
        assertThat(event.properties)
            .containsEntry("api_error_code", "woocommerce_rest_gateway_refund_rejected")
        assertThat(event.properties).containsEntry(RefundFlow.REFUND_FLOW, "server_computed")
    }
}
