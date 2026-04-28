package com.woocommerce.android.aiassistant.tools.handlers

import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject

class OrdersUpdateToolHandler @Inject constructor() : StubToolHandler() {
    override val descriptor = ToolDescriptor(
        name = "orders_update",
        description = """
            Update a single order. Accepts only the `status` field.
            Allowed transitions: on-hold -> processing, processing -> completed.
            Cancellation and refund flows are not supported by this tool.
            At most one write is executed per turn;
            additional write calls in the same turn will be rejected by the runtime.
        """.trimIndent(),
        inputSchema = buildJsonObject { },
        safetyLevel = ToolSafetyLevel.UNSAFE,
    )
}
