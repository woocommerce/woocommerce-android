package com.woocommerce.android.ui.login

import com.google.gson.annotations.SerializedName
import java.util.Base64
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.FinishSecurityKeyChallengePayload

class WebauthnSignedCredential(
    val id: String,
    val rawId: String,
    val type: String,
    val authenticatorAttachment: String,
    val response: WebauthnSignedResponse
) {
    fun asPayload() = FinishSecurityKeyChallengePayload().also {
        it.mId = this.id
        it.mRawId = this.rawId
        it.mType = this.type
        it.mSignature = this.response.signature
        it.mClientDataJSON = this.response.clientDataJSON
        it.mAuthenticatorData = this.response.authenticatorData
        it.mUserHandle = this.response.userHandle.orEmpty()
    }
}

class WebauthnSignedResponse(
    val clientDataJSON: String,
    val authenticatorData: String,
    val signature: String,
    val userHandle: String?
)

class CredentialManagerData(
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

class WebauthnChallengeInfo(
    val challenge: String,
    val rpId: String,
    @SerializedName("two_step_nonce") val twoStepNonce: String,
    val allowCredentials: List<WebauthnCredentialResponse>,
    val timeout: Int
)

class WebauthnCredential(
    val type: String,
    val id: ByteArray,
    val transports: List<String>
)

class WebauthnCredentialResponse(
    val type: String,
    val id: String,
    val transports: List<String>
)
