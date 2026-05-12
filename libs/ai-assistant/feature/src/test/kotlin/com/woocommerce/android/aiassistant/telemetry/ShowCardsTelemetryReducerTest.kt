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

    @Test
    fun `given rejected refs for each validation reason, when reduced, then they only increment rejected count`() {
        val reasons = listOf(
            ShowCardsRejectionReason.OverLimit,
            ShowCardsRejectionReason.UnsupportedFamily,
            ShowCardsRejectionReason.DuplicateRef,
            ShowCardsRejectionReason.MalformedRef,
            ShowCardsRejectionReason.MissingFamily,
            ShowCardsRejectionReason.MissingId,
            ShowCardsRejectionReason.InvalidId,
        )

        reasons.forEach { reason ->
            val counts = ShowCardsTelemetryReducer.reduce(
                ShowCardsStructured(
                    requested = 1,
                    validated = 0,
                    rendered = 0,
                    resolvedRefs = emptyList(),
                    missingRefs = emptyList(),
                    rejectedRefs = listOf(
                        RejectedRef(
                            index = 0,
                            family = "order",
                            id = "123",
                            reason = reason,
                        )
                    ),
                )
            )

            assertThat(counts).describedAs(reason.name).isEqualTo(
                ShowCardsCounts(
                    requestedCount = 1,
                    renderedCount = 0,
                    missingCount = 0,
                    rejectedCount = 1,
                )
            )
        }
    }

    @Test
    fun `given missing refs for each resolver reason, when reduced, then they only increment missing count`() {
        val reasons = listOf(
            ShowCardsRejectionReason.FetchFailed,
            ShowCardsRejectionReason.NotFound,
        )

        reasons.forEach { reason ->
            val counts = ShowCardsTelemetryReducer.reduce(
                ShowCardsStructured(
                    requested = 1,
                    validated = 1,
                    rendered = 0,
                    resolvedRefs = emptyList(),
                    missingRefs = listOf(
                        MissingRef(
                            family = "order",
                            id = "123",
                            reason = reason,
                        )
                    ),
                    rejectedRefs = emptyList(),
                )
            )

            assertThat(counts).describedAs(reason.name).isEqualTo(
                ShowCardsCounts(
                    requestedCount = 1,
                    renderedCount = 0,
                    missingCount = 1,
                    rejectedCount = 0,
                )
            )
        }
    }
}
