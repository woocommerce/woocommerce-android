package com.woocommerce.android.aiassistant.tools.handlers

import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject

class CustomersListToolHandler @Inject constructor() : StubToolHandler() {
    override val descriptor = ToolDescriptor(
        name = "customers_list",
        description = "List customers or get a specific customer by ID.",
        inputSchema = buildJsonObject { },
        safetyLevel = ToolSafetyLevel.SAFE,
    )
}
