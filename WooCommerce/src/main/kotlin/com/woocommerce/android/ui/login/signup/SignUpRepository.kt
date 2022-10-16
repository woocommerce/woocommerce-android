package com.woocommerce.android.ui.login.signup

import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

class SignUpRepository @Inject constructor(
    private val accountStore: AccountStore
) {
    suspend fun createAccount(email: String, password: String): Result<Unit> {
        // 1. Get suggestions for username based on email
        // 2. Create account with first suggested username
        return Result.success(Unit)
    }

    private suspend fun getUserNameForEmail(email: String): List<String> {
        return emptyList()
    }

    data class AccountCreationPayload(
        val email: String,
        val password: String,
        val userName: String
    )
}
