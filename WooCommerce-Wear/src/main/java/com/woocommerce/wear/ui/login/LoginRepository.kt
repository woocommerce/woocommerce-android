package com.woocommerce.wear.ui.login

import javax.inject.Inject

class LoginRepository @Inject constructor() {
    @Suppress("FunctionOnlyReturningConstant")
    fun isUserLoggedIn(): Boolean {
        return false
    }
}
