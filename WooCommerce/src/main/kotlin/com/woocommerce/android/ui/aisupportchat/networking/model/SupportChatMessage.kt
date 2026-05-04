package com.woocommerce.android.ui.aisupportchat.networking.model

import com.google.gson.annotations.SerializedName

data class SupportChatMessage(
    @SerializedName("message_id") val messageId: Long,
    @SerializedName("role") val role: SupportChatRole,
    @SerializedName("content") val content: String,
    @SerializedName("context") val context: SupportChatMessageContext? = null
)
