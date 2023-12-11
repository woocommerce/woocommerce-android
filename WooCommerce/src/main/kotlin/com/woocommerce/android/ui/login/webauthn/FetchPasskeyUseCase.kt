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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FetchPasskeyUseCase {
    operator fun invoke(
        context: Context,
        requestJson: String
    ): Flow<GetCredentialResponse?> = flow {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                emit(createPasskey(context, requestJson))
            } else {
                emit(null)
            }
        }

    }

    @RequiresApi(34)
    private suspend fun CredentialManager.createPasskey(
        context: Context,
        requestJson: String
    ): GetCredentialResponse? {
        val password = GetPasswordOption()
        val publicKeyCred = GetPublicKeyCredentialOption(requestJson)
        val getCredRequest = GetCredentialRequest(
            listOf(password, publicKeyCred)
        )

        try {
            return getCredential(
                request = getCredRequest,
                context = context,
            )
        } catch (e: GetCredentialException) {
            Log.e("Error", e.stackTraceToString())
            return null
        }
    }
}
