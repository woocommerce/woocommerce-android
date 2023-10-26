package com.woocommerce.android.ui.login

import com.google.gson.annotations.SerializedName
import java.util.Base64
import kotlin.math.ceil

data class CredentialManagerData(
    @SerializedName("two_step_nonce") val twoStepNonce: String,
    val challenge: ByteArray,
    val allowCredentials: List<WebauthnCredential>,
    val timeout: Int,
    val rpId: String
) {
    constructor(challengeInfo: WebauthnChallengeInfo) : this(
        challenge = Base64.getUrlDecoder().decode(challengeInfo.challenge),
        twoStepNonce = challengeInfo.twoStepNonce,
        allowCredentials = challengeInfo.allowCredentials.map {
            WebauthnCredential(
                type = it.type,
                id = Base64.getUrlDecoder().decode(it.id),
                transports = it.transports
            )
        },
        timeout = challengeInfo.timeout,
        rpId = challengeInfo.rpId
    )
}

data class WebauthnChallengeInfo(
    val challenge: String,
    val rpId: String,
    @SerializedName("two_step_nonce") val twoStepNonce: String,
    val allowCredentials: List<WebauthnCredentialResponse>,
    val timeout: Int
)

data class WebauthnCredential(
    val type: String,
    val id: ByteArray,
    val transports: List<String>
)

data class WebauthnCredentialResponse(
    val type: String,
    val id: String,
    val transports: List<String>
)
