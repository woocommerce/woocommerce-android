package com.woocommerce.android.ui.login.webauthn

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

class WebauthnSignedCredential(
    val id: String,
    val rawId: String,
    val type: String,
    val response: WebauthnSignedResponse,
    val clientExtensionResults: JsonObject
)

class WebauthnSignedResponse(
    val clientDataJSON: String,
    val authenticatorData: String,
    val signature: String,
    var userHandle: String
)

private val BASE64_FLAG = android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE

fun ByteArray.toBase64(): String {
    return android.util.Base64.encodeToString(this, BASE64_FLAG)
}

fun String.decodeBase64(): ByteArray {
    return android.util.Base64.decode(this, BASE64_FLAG)
}

class CredentialManagerData(
    @SerializedName("two_step_nonce") val twoStepNonce: String,
    val challenge: ByteArray,
    val allowCredentials: List<WebauthnCredential>,
    val timeout: Int,
    val rpId: String
) {
    constructor(challengeInfo: WebauthnChallengeInfo) : this(
        challenge = challengeInfo.challenge.decodeBase64(),
        twoStepNonce = challengeInfo.twoStepNonce,
        allowCredentials = challengeInfo.allowCredentials.map {
            WebauthnCredential(
                type = it.type,
                id = it.id.decodeBase64(),
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
