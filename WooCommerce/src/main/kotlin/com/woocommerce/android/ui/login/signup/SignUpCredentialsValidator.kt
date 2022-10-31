package com.woocommerce.android.ui.login.signup

import android.util.Patterns
import androidx.core.text.isDigitsOnly
import org.wordpress.android.login.LoginEmailFragment
import javax.inject.Inject

class SignUpCredentialsValidator @Inject constructor() {
    private companion object {
        const val PASSWORD_MIN_LENGTH = 7
    }

    fun validateCredentials(
        email: String,
        password: String
    ): SignUpRepository.SignUpError? {
        val invalidCredentialsError = when {
            !isValidEmail(email) -> SignUpRepository.SignUpError.EMAIL_INVALID
            password.length < PASSWORD_MIN_LENGTH -> SignUpRepository.SignUpError.PASSWORD_TOO_SHORT
            password.isDigitsOnly() -> SignUpRepository.SignUpError.PASSWORD_INVALID
            else -> null
        }
        return invalidCredentialsError
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegExPattern = Patterns.EMAIL_ADDRESS
        val matcher = emailRegExPattern.matcher(email)
        return matcher.find() && email.length <= LoginEmailFragment.MAX_EMAIL_LENGTH
    }
}
