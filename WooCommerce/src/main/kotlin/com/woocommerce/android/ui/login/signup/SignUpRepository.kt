package com.woocommerce.android.ui.login.signup

import com.woocommerce.android.OnChangedException
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
    suspend fun createAccount(email: String, password: String): Result<Unit> {
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
                Result.failure(OnChangedException(accountCreatedResult.error, accountCreatedResult.error?.apiError))
            }
            else -> {
                WooLog.w(WooLog.T.LOGIN, "Success creating new account")
                dispatcher.dispatch(
                    AccountActionBuilder.newUpdateAccessTokenAction(
                        UpdateTokenPayload(accountCreatedResult.token)
                    )
                )
                Result.success(Unit)
            }
        }
    }

    // TODO map error based on apiErrorMessage
    enum class SignUpError {
        EMAIL_EXIST,
        USERNAME_EXIST,
        PASSWORD_INVALID,
        UNKNOWN_ERROR
    }
}
