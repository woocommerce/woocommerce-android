package com.woocommerce.android.ui.login

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.gson.annotations.SerializedName
import java.util.Base64
import kotlin.math.ceil

data class CredentialManagerData(
    @SerializedName("two_step_nonce") val twoStepNonce: String,
    val challenge: String,
    val allowCredentials: List<WebauthnCredential>,
    val timeout: Int,
    val rpId: String
) {
    constructor(challengeInfo: WebauthnChallengeInfo) : this(
        challenge = Base64.getUrlDecoder().decode(challengeInfo.challenge).toString(),
        twoStepNonce = challengeInfo.twoStepNonce,
        allowCredentials = challengeInfo.allowCredentials.map {
            it.copy(id = Base64.getUrlDecoder().decode(it.id).toString())
        },
        timeout = challengeInfo.timeout,
        rpId = challengeInfo.rpId
    )
}

data class WebauthnUser(
    val name: String,
    val displayName: String,
    val id: String
)

data class WebauthnRp(
    val name: String,
    val id: String
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

data class WebauthnKeyCredParams(
    val type: String,
    val alg: Int
)

fun String.base64UrlDecode(): ByteArray? {
    var base64 = this.replace("-", "+").replace("_", "/")
    val length = base64.toByteArray().size.toDouble()
    val requiredLength = 4 * ceil(length / 4.0)
    val paddingLength = requiredLength - length
    if (paddingLength > 0) {
        val padding = "=".repeat(paddingLength.toInt())
        base64 += padding
    }
    return Base64.getDecoder().decode(base64)
}
