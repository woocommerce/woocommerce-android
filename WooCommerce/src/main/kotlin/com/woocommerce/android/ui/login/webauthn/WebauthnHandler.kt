package com.woocommerce.android.ui.login.webauthn

import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.common.Transport
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder
import org.wordpress.android.fluxc.store.AccountStore

class WebauthnHandler(
    private val userId: String,
    private val webauthnNonce: String,
    private val dispatcher: Dispatcher
) {
    fun signKeyWithFido(
        activity: ComponentActivity,
        credentialManagerData: CredentialManagerData
    ) {
        val options = PublicKeyCredentialRequestOptions.Builder()
            .setRpId(credentialManagerData.rpId)
            .setAllowList(credentialManagerData.allowCredentials.map(::parseToCredentialDescriptor))
            .setChallenge(credentialManagerData.challenge)
            .setTimeoutSeconds(credentialManagerData.timeout.toDouble())
            .build()

        val fido2ApiClient = Fido.getFido2ApiClient(activity)
        val fidoIntent = fido2ApiClient.getSignPendingIntent(options)

        fidoIntent.addOnSuccessListener { pendingIntent ->
            IntentSenderRequest.Builder(pendingIntent.intentSender)
                .build()
                .let { activity.generateResultLauncher().launch(it) }
        }
    }

    private fun ComponentActivity.generateResultLauncher() = this.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val resultCode = result.resultCode
        val data = result.data
        if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
            if (data.hasExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA)) {
                data.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA)?.let {
                    PublicKeyCredential.deserializeFromBytes(it)
                }?.let { keyCredential ->
                    AccountStore.FinishSecurityKeyChallengePayload().apply {
                        this.mUserId = userId
                        this.mTwoStepNonce = webauthnNonce
                        this.mClientData = keyCredential.toString()
                    }.let { AuthenticationActionBuilder.newFinishSecurityKeyChallengeAction(it) }
                }.let { dispatcher.dispatch(it) }
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
