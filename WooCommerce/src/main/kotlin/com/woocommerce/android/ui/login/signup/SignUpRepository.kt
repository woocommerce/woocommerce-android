package com.woocommerce.android.ui.login.signup

import org.wordpress.android.fluxc.store.SignUpStore
import javax.inject.Inject

class SignUpRepository @Inject constructor(
    private val signUpStore: SignUpStore
) {
    suspend fun createAccount(email: String, password: String): Result<Unit> {
        // 1. Get suggestions for username based on email
        val userNameSuggestionsResult = signUpStore.fetchUserNameSuggestions(email)
        val username = when {
            userNameSuggestionsResult.isError -> email
            else -> userNameSuggestionsResult.suggestions.first()
        }
        signUpStore.createWpAccount(email, password, username)
        return Result.success(Unit)
    }
}
