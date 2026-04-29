package com.woocommerce.android.aiassistant.tools.handlers

import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject

class OrdersGetToolHandler @Inject constructor() : StubToolHandler() {
    override val descriptor = ToolDescriptor(
        name = "orders_get",
        description = "Get a single order by ID.",
        inputSchema = buildJsonObject { },
        safetyLevel = ToolSafetyLevel.SAFE,
    )
}
