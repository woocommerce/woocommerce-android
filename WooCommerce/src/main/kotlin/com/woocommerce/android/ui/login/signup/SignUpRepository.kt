package com.woocommerce.android.ui.login.signup

import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload
import org.wordpress.android.fluxc.store.signup.SignUpStore
import javax.inject.Inject

class SignUpRepository @Inject constructor(
    private val signUpStore: SignUpStore,
    private val dispatcher: Dispatcher
) {
    private companion object {
        const val EMAIL_EXIST_API_ERROR = "email_exists"
        const val EMAIL_INVALID_API_ERROR = "email_invalid"
        const val PASSWORD_INVALID_API_ERROR = "password_invalid"
    }

    suspend fun createAccount(email: String, password: String): AccountCreationResult {
        // 1. Get suggestions for username based on email
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
                dispatcher.dispatch(
                    AccountActionBuilder.newUpdateAccessTokenAction(
                        UpdateTokenPayload(accountCreatedResult.token)
                    )
                )
                AccountCreationSuccess
            }
        }
    }

    sealed class AccountCreationResult
    object AccountCreationSuccess : AccountCreationResult()
    data class AccountCreationError(
        val error: SignUpError
    ) : AccountCreationResult()

    private fun String?.toSignUpError() =
        when {
            this == EMAIL_EXIST_API_ERROR -> SignUpError.EMAIL_EXIST
            this == EMAIL_INVALID_API_ERROR -> SignUpError.EMAIL_INVALID
            this == PASSWORD_INVALID_API_ERROR -> SignUpError.PASSWORD_INVALID
            else -> SignUpError.UNKNOWN_ERROR
        }

    enum class SignUpError {
        EMAIL_EXIST,
        EMAIL_INVALID,
        PASSWORD_INVALID,
        UNKNOWN_ERROR
    }
}
