package com.woocommerce.android.ui.aisupportchat.networking.model

import com.google.gson.annotations.SerializedName

data class SupportChatMessageContext(
    @SerializedName("sources") val sources: List<SupportChatSource> = emptyList(),
    @SerializedName("flags") val flags: SupportChatFlags? = null,
    @SerializedName("support_area") val supportArea: SupportChatSupportArea? = null,
    @SerializedName("is_resolved") val isResolved: Boolean = false
)
