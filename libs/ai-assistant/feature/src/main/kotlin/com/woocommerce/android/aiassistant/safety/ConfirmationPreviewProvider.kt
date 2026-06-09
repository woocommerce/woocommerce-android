package com.woocommerce.android.aiassistant.safety

import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.safety.ConfirmationRequest

internal data class ConfirmationPreviewContext(
    val request: ConfirmationRequest,
    val descriptor: ToolDescriptor,
)

internal interface ConfirmationPreviewProvider {
    val key: String
    val priority: Int
    fun canPreview(context: ConfirmationPreviewContext): Boolean
    suspend fun buildPreview(context: ConfirmationPreviewContext): ConfirmationPreview
}
