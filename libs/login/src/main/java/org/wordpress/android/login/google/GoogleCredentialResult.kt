package org.wordpress.android.login.google

sealed interface GoogleCredentialResult {
    data class Success(
        val idToken: String,
        val email: String,
        val displayName: String,
        val photoUrl: String
    ) : GoogleCredentialResult

    data class Cancelled(val errorType: String, val errorMessage: String?) : GoogleCredentialResult

    data class Failure(val errorType: String, val errorMessage: String?) : GoogleCredentialResult

    companion object {
        const val ERROR_TYPE_UNEXPECTED_CREDENTIAL = "unexpected_credential"
        const val ERROR_TYPE_INVALID_ID_TOKEN = "invalid_id_token"
    }
}
