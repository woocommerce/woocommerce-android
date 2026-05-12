package com.woocommerce.android.aiassistant.telemetry

import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsStructured

internal data class ShowCardsCounts(
    val requestedCount: Int,
    val renderedCount: Int,
    val missingCount: Int,
    val rejectedCount: Int,
)

internal object ShowCardsTelemetryReducer {
    fun reduce(structured: ShowCardsStructured): ShowCardsCounts =
        ShowCardsCounts(
            requestedCount = structured.requested,
            renderedCount = structured.rendered,
            missingCount = structured.missingRefs.size,
            rejectedCount = structured.rejectedRefs.size,
        )
}
