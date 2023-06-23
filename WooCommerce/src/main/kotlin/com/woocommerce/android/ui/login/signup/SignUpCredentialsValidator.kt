package com.woocommerce.android.ui.login.signup

import androidx.core.text.isDigitsOnly
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError.PASSWORD_INVALID
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError.PASSWORD_TOO_SHORT
import com.woocommerce.android.util.StringUtils
import javax.inject.Inject

class SignUpCredentialsValidator @Inject constructor() {
    private companion object {
        const val PASSWORD_MIN_LENGTH = 6
    }

    fun isEmailValid(email: String): Boolean = StringUtils.isValidEmail(email)

    fun validatePassword(password: String): SignUpError? = when {
        password.length < PASSWORD_MIN_LENGTH -> PASSWORD_TOO_SHORT
        password.isDigitsOnly() -> PASSWORD_INVALID
        else -> null
    }
}
