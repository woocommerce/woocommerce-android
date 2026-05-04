package com.woocommerce.android.ui.aisupportchat.networking.model

import com.google.gson.annotations.SerializedName

data class SupportChatSource(
    @SerializedName("title") val title: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("heading") val heading: String?,
    @SerializedName("content") val content: String?
)
