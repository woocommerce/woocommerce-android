package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor

fun interface ToolCatalogSelector {
    fun select(scope: ToolScope, fullRegistry: List<ToolDescriptor>): CatalogSnapshot
}
