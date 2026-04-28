package com.woocommerce.android.aiassistant.tools.handlers

import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject

class ProductsBulkUpdateToolHandler @Inject constructor() : StubToolHandler() {
    override val descriptor = ToolDescriptor(
        name = "products_bulk_update",
        description = """
            Update multiple products. Accepts only these fields per product: regular_price, manage_stock, stock_quantity, stock_status, status.
            Bulk writes require confirmation. At most one write operation (single or bulk) is executed per turn.
        """.trimIndent(),
        inputSchema = buildJsonObject { },
        safetyLevel = ToolSafetyLevel.UNSAFE,
    )
}
