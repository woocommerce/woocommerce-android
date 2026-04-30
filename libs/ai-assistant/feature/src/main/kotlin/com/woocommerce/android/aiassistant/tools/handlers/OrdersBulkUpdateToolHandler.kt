package com.woocommerce.android.aiassistant.tools.handlers

import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject

class OrdersBulkUpdateToolHandler @Inject constructor() : StubToolHandler() {
    override val descriptor = ToolDescriptor(
        name = "orders_bulk_update",
        description = """
            Update multiple orders. Accepts only the `status` field per order.
            Allowed transitions: on-hold -> processing, processing -> completed.
            Bulk writes require confirmation. At most one write operation (single or bulk) is executed per turn.
        """.trimIndent(),
        inputSchema = buildJsonObject { },
        safetyLevel = ToolSafetyLevel.UNSAFE,
    )
}
