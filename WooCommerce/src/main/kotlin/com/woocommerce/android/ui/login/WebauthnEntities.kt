package com.woocommerce.android.ui.login

import com.google.gson.annotations.SerializedName


data class CredentialManagerData(
    @SerializedName("two_step_nonce") val twoStepNonce: String,
    @SerializedName("user.name") val username: String,
    @SerializedName("user.id") val userId: Long,
    val challenge: String,
    val rpId: String,
    val allowCredentials: List<WebauthnCredential>,
    val timeout: Int,
) {
    constructor(challengeInfo: WebauthnChallengeInfo, userId: Long, username: String) : this(
        username = username,
        userId = userId,
        challenge = challengeInfo.challenge,
        rpId = challengeInfo.rpId,
        twoStepNonce = challengeInfo.twoStepNonce,
        allowCredentials = challengeInfo.allowCredentials,
        timeout = challengeInfo.timeout
    )
}
data class WebauthnChallengeInfo(
    val challenge: String,
    val rpId: String,
    @SerializedName("two_step_nonce") val twoStepNonce: String,
    val allowCredentials: List<WebauthnCredential>,
    val timeout: Int
)

data class WebauthnCredential(
    val type: String,
    val id: String,
    val transports: List<String>
)
