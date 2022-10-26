package com.woocommerce.android.ui.login.signup

import android.util.Patterns
import androidx.core.text.isDigitsOnly
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError.EMAIL_EXIST
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError.EMAIL_INVALID
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError.PASSWORD_INVALID
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError.PASSWORD_TOO_SHORT
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError.UNKNOWN_ERROR
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.dispatchAndAwait
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload
import org.wordpress.android.fluxc.store.signup.SignUpStore
import org.wordpress.android.login.LoginEmailFragment
import javax.inject.Inject

class SignUpRepository @Inject constructor(
    private val signUpStore: SignUpStore,
    private val dispatcher: Dispatcher
) {
    private companion object {
        const val EMAIL_EXIST_API_ERROR = "email_exists"
        const val EMAIL_INVALID_API_ERROR = "email_invalid"
        const val PASSWORD_INVALID_API_ERROR = "password_invalid"
        const val PASSWORD_MIN_LENGTH = 7
    }

    suspend fun createAccount(email: String, password: String): AccountCreationResult {
        val invalidCredentialsError = validateCredentials(email, password)
        if (invalidCredentialsError != null) {
            return AccountCreationError(invalidCredentialsError)
        }

        WooLog.d(WooLog.T.LOGIN, "Fetching suggestions for username")
        val userNameSuggestionsResult = signUpStore.fetchUserNameSuggestions(email)
        val username = when {
            userNameSuggestionsResult.isError -> email
            else -> userNameSuggestionsResult.suggestions.first()
        }

        WooLog.d(WooLog.T.LOGIN, "Creating new WP account for: $email, $username")
        val accountCreatedResult = signUpStore.createWpAccount(email, password, username)
        return when {
            accountCreatedResult.isError -> {
                WooLog.w(WooLog.T.LOGIN, "Error creating new WP account: ${accountCreatedResult.error.apiError}")
                AccountCreationError(accountCreatedResult.error.apiError.toSignUpError())
            }
            else -> {
                WooLog.w(WooLog.T.LOGIN, "Success creating new account")
                dispatcher.dispatchAndAwait<UpdateTokenPayload, OnAuthenticationChanged>(
                    AccountActionBuilder.newUpdateAccessTokenAction(UpdateTokenPayload(accountCreatedResult.token))
                ).let { updateTokenResult ->
                    if (updateTokenResult.isError) {
                        WooLog.w(WooLog.T.LOGIN, "Error updating token: ${updateTokenResult.error.message}")
                        AccountCreationError(UNKNOWN_ERROR)
                    } else {
                        dispatcher.dispatchAndAwait<Void, OnAccountChanged>(
                            AccountActionBuilder.newFetchAccountAction()
                        ).let { accountFetchResult ->
                            if (accountFetchResult.isError) {
                                WooLog.w(
                                    WooLog.T.LOGIN,
                                    message = "Error fetching the user: ${accountFetchResult.error.message}"
                                )
                                AccountCreationError(UNKNOWN_ERROR)
                            } else {
                                AccountCreationSuccess
                            }
                        }
                    }
                }
            }
        }
    }

    private fun validateCredentials(
        email: String,
        password: String
    ): SignUpError? {
        val invalidCredentialsError = when {
            !isValidEmail(email) -> EMAIL_INVALID
            password.length < PASSWORD_MIN_LENGTH -> PASSWORD_TOO_SHORT
            password.isDigitsOnly() -> PASSWORD_INVALID
            else -> null
        }
        return invalidCredentialsError
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegExPattern = Patterns.EMAIL_ADDRESS
        val matcher = emailRegExPattern.matcher(email)
        return matcher.find() && email.length <= LoginEmailFragment.MAX_EMAIL_LENGTH
    }

    sealed class AccountCreationResult
    object AccountCreationSuccess : AccountCreationResult()
    data class AccountCreationError(
        val error: SignUpError
    ) : AccountCreationResult()

    private fun String?.toSignUpError() =
        when {
            this == EMAIL_EXIST_API_ERROR -> EMAIL_EXIST
            this == EMAIL_INVALID_API_ERROR -> EMAIL_INVALID
            this == PASSWORD_INVALID_API_ERROR -> PASSWORD_INVALID
            else -> UNKNOWN_ERROR
        }

    enum class SignUpError {
        EMAIL_EXIST,
        EMAIL_INVALID,
        PASSWORD_INVALID,
        PASSWORD_TOO_SHORT,
        UNKNOWN_ERROR
    }
}
