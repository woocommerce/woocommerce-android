package com.woocommerce.android.ui.login.signup

import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError.EMAIL_EXIST
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError.EMAIL_INVALID
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError.PASSWORD_INVALID
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError.UNKNOWN_ERROR
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.dispatchAndAwait
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload
import org.wordpress.android.fluxc.store.account.SignUpStore
import javax.inject.Inject

class SignUpRepository @Inject constructor(
    private val signUpStore: SignUpStore,
    private val dispatcher: Dispatcher,
) {
    private companion object {
        const val EMAIL_EXIST_API_ERROR = "email_exists"
        const val EMAIL_INVALID_API_ERROR = "email_invalid"
        const val PASSWORD_INVALID_API_ERROR = "password_invalid"
        const val USERNAME_INVALID_API_ERROR = "username_invalid"
        const val BLACKLISTED_WORDING_ON_USERNAME = "wordpress"
    }

    suspend fun createAccount(email: String, password: String): AccountCreationResult {
        WooLog.d(WooLog.T.LOGIN, "Fetching suggestions for username")
        val userNameSuggestionsResult = signUpStore.fetchUserNameSuggestions(
            email.lowercase().replace(BLACKLISTED_WORDING_ON_USERNAME, "")
        )
        val username = when {
            userNameSuggestionsResult.isError -> email.substring(0, email.indexOf("@"))
            else -> userNameSuggestionsResult.suggestions.first()
        }

        WooLog.d(WooLog.T.LOGIN, "Creating new WP account for: $email, $username")
        val accountCreatedResult = signUpStore.createWpAccount(email, password, username)
        return when {
            accountCreatedResult.isError -> {
                WooLog.w(WooLog.T.LOGIN, "Error creating new WP account: ${accountCreatedResult.error.apiError}")
                AccountCreationError(accountCreatedResult.error.apiError.toSignUpError())
            }
            else -> onAccountCreationSuccess(username, accountCreatedResult)
        }
    }

    private suspend fun onAccountCreationSuccess(
        username: String,
        accountCreatedResult: SignUpStore.CreateWpAccountResult
    ): AccountCreationResult {
        dispatcher.dispatchAndAwait<UpdateTokenPayload, OnAuthenticationChanged>(
            AccountActionBuilder.newUpdateAccessTokenAction(UpdateTokenPayload(accountCreatedResult.token))
        ).let { updateTokenResult ->
            return if (updateTokenResult.isError) {
                WooLog.w(WooLog.T.LOGIN, "Error updating token: ${updateTokenResult.error.message}")
                AccountCreationError(UNKNOWN_ERROR)
            } else {
                onTokenUpdatedSuccessfully(username)
            }
        }
    }

    private suspend fun onTokenUpdatedSuccessfully(username: String): AccountCreationResult {
        return dispatcher.dispatchAndAwait<Void, OnAccountChanged>(
            AccountActionBuilder.newFetchAccountAction()
        ).let { accountFetchResult ->
            if (accountFetchResult.isError) {
                WooLog.w(WooLog.T.LOGIN, message = "Error fetching the user: ${accountFetchResult.error.message}")
                // Persist the username manually and ignore the error
                val account = AccountModel().apply {
                    userName = username
                }
                dispatcher.dispatchAndAwait<AccountModel, OnAccountChanged>(
                    AccountActionBuilder.newUpdateAccountAction(account)
                )
            }

            WooLog.w(WooLog.T.LOGIN, "Success creating new account")
            AccountCreationSuccess
        }
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
            this == USERNAME_INVALID_API_ERROR -> SignUpError.USERNAME_INVALID
            else -> UNKNOWN_ERROR
        }

    enum class SignUpError {
        EMAIL_EXIST,
        EMAIL_INVALID,
        PASSWORD_INVALID,
        PASSWORD_TOO_SHORT,
        USERNAME_INVALID,
        UNKNOWN_ERROR
    }
}
