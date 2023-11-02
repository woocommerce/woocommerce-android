package com.woocommerce.android.ui.login.webauthn

import android.content.Context
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.common.Transport
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType
import com.woocommerce.android.extensions.decodeBase64
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnChallengeInfo
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnCredentialResponse

class RequestPasskeyUseCase {
    operator fun invoke(
        context: Context,
        challengeInfo: WebauthnChallengeInfo,
        onPasskeyRequestReady: (IntentSenderRequest) -> Unit
    ) {
        val options = PublicKeyCredentialRequestOptions.Builder()
            .setRpId(challengeInfo.rpId)
            .setAllowList(challengeInfo.allowCredentials.map(::parseToCredentialDescriptor))
            .setChallenge(challengeInfo.challenge.decodeBase64())
            .setTimeoutSeconds(challengeInfo.timeout.toDouble())
            .build()

        Fido.getFido2ApiClient(context)
            .getSignPendingIntent(options)
            .addOnSuccessListener {
                val intentSender = IntentSenderRequest
                    .Builder(it.intentSender)
                    .build()

                onPasskeyRequestReady(intentSender)
            }
    }

    private fun parseToCredentialDescriptor(credential: WebauthnCredentialResponse) =
        PublicKeyCredentialDescriptor(
            PublicKeyCredentialType.PUBLIC_KEY.toString(),
            credential.id.decodeBase64(),
            allTransports
        )

    private val allTransports = listOf(
        Transport.USB,
        Transport.NFC,
        Transport.BLUETOOTH_LOW_ENERGY,
        Transport.HYBRID,
        Transport.INTERNAL
    )
}
