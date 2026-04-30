package com.woocommerce.android.aiassistant.tools.handlers

import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject

class ProductsGetToolHandler @Inject constructor() : StubToolHandler() {
    override val descriptor = ToolDescriptor(
        name = "products_get",
        description = "Get a single product by ID.",
        inputSchema = buildJsonObject { },
        safetyLevel = ToolSafetyLevel.SAFE,
    )
}
