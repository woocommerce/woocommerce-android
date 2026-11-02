package org.wordpress.android.login.google

import android.app.Activity
import android.content.Context
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

class GoogleCredentialClient(context: Context, private val serverClientId: String) {
    private val credentialManager = CredentialManager.create(context.applicationContext)

    fun interface Callback {
        fun onResult(result: GoogleCredentialResult)
    }

    fun requestIdToken(activity: Activity, cancellationSignal: CancellationSignal, callback: Callback) {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId).build())
            .build()

        credentialManager.getCredentialAsync(
            context = activity,
            request = request,
            cancellationSignal = cancellationSignal,
            executor = ContextCompat.getMainExecutor(activity),
            callback = object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {
                    callback.onResult(result.credential.toGoogleCredentialResult())
                }

                override fun onError(e: GetCredentialException) {
                    callback.onResult(e.toGoogleCredentialResult())
                }
            }
        )
    }

    fun clearCredentialState() {
        credentialManager.clearCredentialStateAsync(
            request = ClearCredentialStateRequest(),
            cancellationSignal = null,
            executor = Runnable::run,
            callback = object : CredentialManagerCallback<Void?, ClearCredentialException> {
                override fun onResult(result: Void?) = Unit

                override fun onError(e: ClearCredentialException) {
                    AppLog.e(T.NUX, "Clearing Google credential state failed: ${e.type} - ${e.errorMessage}")
                }
            }
        )
    }
}

internal fun Credential.toGoogleCredentialResult(): GoogleCredentialResult {
    if (this !is CustomCredential || type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        return GoogleCredentialResult.Failure(
            errorType = GoogleCredentialResult.ERROR_TYPE_UNEXPECTED_CREDENTIAL,
            errorMessage = type
        )
    }

    return try {
        val credential = GoogleIdTokenCredential.createFrom(data)
        val email = credential.email
        if (email.isNullOrBlank()) {
            return GoogleCredentialResult.Failure(
                errorType = GoogleCredentialResult.ERROR_TYPE_INVALID_ID_TOKEN,
                errorMessage = "Google ID token is missing an email address"
            )
        }
        GoogleCredentialResult.Success(
            idToken = credential.idToken,
            email = email,
            displayName = credential.displayName.orEmpty(),
            photoUrl = credential.profilePictureUri?.toString().orEmpty()
        )
    } catch (e: GoogleIdTokenParsingException) {
        GoogleCredentialResult.Failure(
            errorType = GoogleCredentialResult.ERROR_TYPE_INVALID_ID_TOKEN,
            errorMessage = e.message
        )
    }
}

internal fun GetCredentialException.toGoogleCredentialResult(): GoogleCredentialResult =
    when (this) {
        is GetCredentialCancellationException -> GoogleCredentialResult.Cancelled(
            errorType = type,
            errorMessage = errorMessage?.toString()
        )

        is GetCredentialInterruptedException -> GoogleCredentialResult.Failure(
            errorType = type,
            errorMessage = errorMessage?.toString(),
            isNetworkError = true
        )

        is NoCredentialException -> GoogleCredentialResult.Failure(
            errorType = type,
            errorMessage = errorMessage?.toString()
        )

        else -> GoogleCredentialResult.Failure(
            errorType = type,
            errorMessage = errorMessage?.toString()
        )
    }
