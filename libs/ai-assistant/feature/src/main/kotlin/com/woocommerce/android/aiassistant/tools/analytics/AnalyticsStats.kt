package com.woocommerce.android.aiassistant.tools.analytics

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal data class AnalyticsStats(
    val totals: JsonElement?,
    val intervals: List<JsonObject>?,
)
