package com.woocommerce.android.ui.aisupportchat.networking.model

import com.google.gson.annotations.SerializedName

data class SupportChatFlags(
    @SerializedName("forward_to_human_support") val forwardToHumanSupport: Boolean = false,
    @SerializedName("canned_response") val cannedResponse: Boolean = false,
    @SerializedName("logged_in") val loggedIn: Boolean = false,
    @SerializedName("branch") val branch: String? = null
)
