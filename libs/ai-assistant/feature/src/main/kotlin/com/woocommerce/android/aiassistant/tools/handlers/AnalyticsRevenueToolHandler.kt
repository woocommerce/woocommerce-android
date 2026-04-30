package com.woocommerce.android.aiassistant.tools.handlers

import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject

class AnalyticsRevenueToolHandler @Inject constructor() : StubToolHandler() {
    override val descriptor = ToolDescriptor(
        name = "analytics_revenue",
        description = "Get revenue analytics for a date range.",
        inputSchema = buildJsonObject { },
        safetyLevel = ToolSafetyLevel.SAFE,
    )
}
