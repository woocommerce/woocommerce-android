package com.woocommerce.android.aiassistant.telemetry

import com.woocommerce.android.aiassistant.tools.handlers.cards.MissingRef
import com.woocommerce.android.aiassistant.tools.handlers.cards.RejectedRef
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsRejectionReason
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsStructured
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ShowCardsTelemetryReducerTest {
    @Test
    fun `given structured show cards result, when reduced, then only aggregate counts are returned`() {
        val structured = ShowCardsStructured(
            requested = 5,
            validated = 4,
            rendered = 2,
            resolvedRefs = emptyList(),
            missingRefs = listOf(
                MissingRef("order", "123", ShowCardsRejectionReason.NotFound),
            ),
            rejectedRefs = listOf(
                RejectedRef(0, reason = ShowCardsRejectionReason.MissingFamily),
                RejectedRef(1, reason = ShowCardsRejectionReason.DuplicateRef),
            ),
        )

        val counts = ShowCardsTelemetryReducer.reduce(structured)

        assertThat(counts).isEqualTo(
            ShowCardsCounts(
                requestedCount = 5,
                renderedCount = 2,
                missingCount = 1,
                rejectedCount = 2,
            )
        )
    }
}
