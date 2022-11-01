package com.woocommerce.android.ui.login.signup

import android.util.Patterns
import androidx.core.text.isDigitsOnly
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError
import org.wordpress.android.login.LoginEmailFragment
import javax.inject.Inject

class SignUpCredentialsValidator @Inject constructor() {
    private companion object {
        const val PASSWORD_MIN_LENGTH = 7
    }

    fun validateCredentials(
        email: String,
        password: String
    ): SignUpError? = when {
        !isValidEmail(email) -> SignUpError.EMAIL_INVALID
        password.length < PASSWORD_MIN_LENGTH -> SignUpError.PASSWORD_TOO_SHORT
        password.isDigitsOnly() -> SignUpError.PASSWORD_INVALID
        else -> null
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegExPattern = Patterns.EMAIL_ADDRESS
        val matcher = emailRegExPattern.matcher(email)
        return matcher.find() && email.length <= LoginEmailFragment.MAX_EMAIL_LENGTH
    }
}
