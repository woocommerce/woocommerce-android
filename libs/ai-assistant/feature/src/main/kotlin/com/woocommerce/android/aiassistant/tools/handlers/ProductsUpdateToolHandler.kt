package com.woocommerce.android.aiassistant.tools.handlers

import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject

class ProductsUpdateToolHandler @Inject constructor() : StubToolHandler() {
    override val descriptor = ToolDescriptor(
        name = "products_update",
        description = """
            Update a single product. Accepts only these fields: regular_price, manage_stock, stock_quantity, stock_status, status.
            At most one write is executed per turn; additional write calls in the same turn will be rejected by the runtime.
        """.trimIndent(),
        inputSchema = buildJsonObject { },
        safetyLevel = ToolSafetyLevel.UNSAFE,
    )
}
