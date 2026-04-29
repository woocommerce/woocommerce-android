package com.woocommerce.android.aiassistant.tools.handlers

import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject

class ProductsListToolHandler @Inject constructor() : StubToolHandler() {
    override val descriptor = ToolDescriptor(
        name = "products_list",
        description = "List and search products. Supports filtering by status, category, and stock.",
        inputSchema = buildJsonObject { },
        safetyLevel = ToolSafetyLevel.SAFE,
    )
}
