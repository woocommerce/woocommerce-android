package com.woocommerce.android.ui.login.webauthn

import android.content.Context
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.exceptions.GetCredentialException

class FetchPasskeyUseCase {


    @RequiresApi(34)
    private suspend fun CredentialManager.createPasskey(
        context: Context,
        requestJson: String
    ) {
        val password = GetPasswordOption()
        val publicKeyCred = GetPublicKeyCredentialOption(requestJson)
        val getCredRequest = GetCredentialRequest(
            listOf(password, publicKeyCred)
        )

        try {
            val result: GetCredentialResponse = getCredential(
                request = getCredRequest,
                context = context,
            )
            result.apply { }
        } catch (e: GetCredentialException) {
            Log.e("Error", e.stackTraceToString())
            // handle error
        }
    }
}
