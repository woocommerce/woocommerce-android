package org.wordpress.android.login.google

import android.net.Uri
import android.os.Bundle
import androidx.credentials.CustomCredential
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GoogleCredentialResultMappingTest {
    @Test
    fun `given Google ID token credential, when mapping, then return success with account details`() {
        val credential = GoogleIdTokenCredential.Builder()
            .setId("merchant@example.com")
            .setIdToken("id-token")
            .setDisplayName("Merchant Name")
            .setProfilePictureUri(Uri.parse("https://lh3.googleusercontent.com/a/photo/s96-c/photo.jpg"))
            .build()

        val result = CustomCredential(credential.type, credential.data).toGoogleCredentialResult()

        assertThat(result).isEqualTo(
            GoogleCredentialResult.Success(
                idToken = "id-token",
                email = "merchant@example.com",
                displayName = "Merchant Name",
                photoUrl = "https://lh3.googleusercontent.com/a/photo/s96-c/photo.jpg"
            )
        )
    }

    @Test
    fun `given Google ID token credential without optional fields, when mapping, then return empty strings`() {
        val credential = GoogleIdTokenCredential.Builder()
            .setId("merchant@example.com")
            .setIdToken("id-token")
            .build()

        val result = CustomCredential(credential.type, credential.data).toGoogleCredentialResult()

        assertThat(result).isEqualTo(
            GoogleCredentialResult.Success(
                idToken = "id-token",
                email = "merchant@example.com",
                displayName = "",
                photoUrl = ""
            )
        )
    }

    @Test
    fun `given non Google credential, when mapping, then return unexpected credential failure`() {
        val result = PasswordCredential("merchant@example.com", "password").toGoogleCredentialResult()

        assertThat(result).isEqualTo(
            GoogleCredentialResult.Failure(
                errorType = GoogleCredentialResult.ERROR_TYPE_UNEXPECTED_CREDENTIAL,
                errorMessage = PasswordCredential.TYPE_PASSWORD_CREDENTIAL
            )
        )
    }

    @Test
    fun `given Google credential with malformed data, when mapping, then return invalid ID token failure`() {
        val result = CustomCredential(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, Bundle())
            .toGoogleCredentialResult()

        assertThat(result).isInstanceOf(GoogleCredentialResult.Failure::class.java)
        assertThat((result as GoogleCredentialResult.Failure).errorType)
            .isEqualTo(GoogleCredentialResult.ERROR_TYPE_INVALID_ID_TOKEN)
    }

    @Test
    fun `given user dismissed the account picker, when mapping exception, then return cancelled`() {
        val exception = GetCredentialCancellationException("User dismissed")

        val result = exception.toGoogleCredentialResult()

        assertThat(result).isEqualTo(
            GoogleCredentialResult.Cancelled(errorType = exception.type, errorMessage = "User dismissed")
        )
    }

    @Test
    fun `given no eligible Google account, when mapping exception, then return failure`() {
        val exception = NoCredentialException("No credentials available")

        val result = exception.toGoogleCredentialResult()

        assertThat(result).isEqualTo(
            GoogleCredentialResult.Failure(errorType = exception.type, errorMessage = "No credentials available")
        )
    }

    @Test
    fun `given interrupted request, when mapping exception, then return network failure`() {
        val exception = GetCredentialInterruptedException()

        val result = exception.toGoogleCredentialResult()

        assertThat(result).isEqualTo(
            GoogleCredentialResult.Failure(errorType = exception.type, errorMessage = null, isNetworkError = true)
        )
    }
}
