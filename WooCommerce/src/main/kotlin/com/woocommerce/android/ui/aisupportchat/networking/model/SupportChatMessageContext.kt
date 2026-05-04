package com.woocommerce.android.ui.aisupportchat.networking.model

import com.google.gson.annotations.SerializedName

data class SupportChatMessageContext(
    @SerializedName("sources") val sources: List<SupportChatSource>? = null,
    @SerializedName("flags") val flags: SupportChatFlags? = null
)
