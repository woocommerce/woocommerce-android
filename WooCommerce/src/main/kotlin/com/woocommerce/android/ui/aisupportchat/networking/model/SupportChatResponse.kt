package com.woocommerce.android.ui.aisupportchat.networking.model

import com.google.gson.annotations.SerializedName

data class SupportChatResponse(
    @SerializedName("chat_id") val chatId: Long,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("bot_slug") val botSlug: String,
    @SerializedName("bot_version") val botVersion: String,
    @SerializedName("messages") val messages: List<SupportChatMessage> = emptyList()
)
