package com.woocommerce.android.ui.login.webauthn

import android.content.Context
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.common.Transport
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType
import javax.inject.Inject
import org.wordpress.android.fluxc.Dispatcher

typealias OnCredentialsAvailable = (PublicKeyCredential) -> Unit

interface PasskeyResultReceiver {
    fun requestPasskeyWith(
        intentSender: IntentSenderRequest,
        userId: String,
        webauthnNonce: String
    )
}

class FetchPasskeyUseCase @Inject constructor(
    private val dispatcher: Dispatcher
) {
    operator fun invoke(
        context: Context,
        receiver: PasskeyResultReceiver,
        credentialManagerData: CredentialManagerData,
        userId: String
    ) {
        val options = PublicKeyCredentialRequestOptions.Builder()
            .setRpId(credentialManagerData.rpId)
            .setAllowList(credentialManagerData.allowCredentials.map(::parseToCredentialDescriptor))
            .setChallenge(credentialManagerData.challenge)
            .setTimeoutSeconds(credentialManagerData.timeout.toDouble())
            .build()

        val fido2ApiClient = Fido.getFido2ApiClient(context)
        val fidoIntent = fido2ApiClient.getSignPendingIntent(options)

        fidoIntent.addOnSuccessListener { pendingIntent ->
            IntentSenderRequest.Builder(pendingIntent.intentSender)
                .build()
                .let {
                    receiver.requestPasskeyWith(
                        intentSender = it,
                        userId = userId,
                        webauthnNonce = credentialManagerData.twoStepNonce
                    )
                }
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
