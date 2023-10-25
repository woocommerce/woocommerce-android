package com.woocommerce.android.ui.login

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.gson.annotations.SerializedName
import java.util.Base64

data class CredentialManagerData(
    @SerializedName("two_step_nonce") val twoStepNonce: String,
    val challenge: String,
    val rpId: String,
    val allowCredentials: List<WebauthnCredential>,
    val timeout: Int,
    val user: WebauthnUser
) {
    @RequiresApi(Build.VERSION_CODES.O)
    constructor(challengeInfo: WebauthnChallengeInfo, userId: Long, username: String) : this(
        challenge = String(Base64.getUrlDecoder().decode(challengeInfo.challenge)),
        rpId = challengeInfo.rpId,
        twoStepNonce = challengeInfo.twoStepNonce,
        allowCredentials = challengeInfo.allowCredentials,
        timeout = challengeInfo.timeout,
        user = WebauthnUser(
            name = username,
            id = userId
        )
    )
}

data class WebauthnUser(
    val name: String,
    val id: Long
)

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
