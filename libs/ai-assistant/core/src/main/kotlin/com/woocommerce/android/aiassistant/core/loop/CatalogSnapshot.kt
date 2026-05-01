package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor

data class CatalogSnapshot(
    val scope: ToolScope,
    val tools: List<ToolDescriptor>,
)
