package com.woocommerce.android.ui.login.webauthn

import android.content.Context
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.common.Transport
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType

class RequestPasskeyUseCase {
    operator fun invoke(
        context: Context,
        credentialManagerData: CredentialManagerData,
        onPasskeyRequestReady: (IntentSenderRequest) -> Unit
    ) {
        val options = PublicKeyCredentialRequestOptions.Builder()
            .setRpId(credentialManagerData.rpId)
            .setAllowList(credentialManagerData.allowCredentials.map(::parseToCredentialDescriptor))
            .setChallenge(credentialManagerData.challenge)
            .setTimeoutSeconds(credentialManagerData.timeout.toDouble())
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

    private fun parseToCredentialDescriptor(credential: WebauthnCredential) =
        PublicKeyCredentialDescriptor(
            PublicKeyCredentialType.PUBLIC_KEY.toString(),
            credential.id,
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
