package com.woocommerce.android.aiassistant.tools.handlers

import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.chat.inputSchema
import javax.inject.Inject

class ShowCardsToolHandler @Inject constructor() : StubToolHandler() {
    override val descriptor = ToolDescriptor(
        name = "show_cards",
        description = "Show entity cards in the UI for orders or products selected by the assistant.",
        inputSchema = inputSchema {
            string("family")
        },
        safetyLevel = ToolSafetyLevel.SAFE,
    )
}
