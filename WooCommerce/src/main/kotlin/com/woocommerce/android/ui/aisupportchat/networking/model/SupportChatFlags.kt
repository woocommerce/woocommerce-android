package com.woocommerce.android.ui.aisupportchat.networking.model

import com.google.gson.annotations.SerializedName

// All constructor parameters must keep defaults so Gson uses Kotlin's synthetic no-arg constructor and preserves
// these defaults when fields are missing from the response.
data class SupportChatFlags(
    @SerializedName("forward_to_human_support") val forwardToHumanSupport: Boolean = false,
    @SerializedName("canned_response") val cannedResponse: Boolean = false,
    @SerializedName("logged_in") val loggedIn: Boolean = false,
    @SerializedName("branch") val branch: String? = null
)
