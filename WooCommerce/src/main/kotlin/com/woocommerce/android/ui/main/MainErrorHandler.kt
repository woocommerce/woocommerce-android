package com.woocommerce.android.ui.main

import javax.inject.Inject

/**
 * This class provides action-specific error handling and generic error messaging.
 */
class MainErrorHandler @Inject constructor(val uiResolver: MainUIMessageResolver) : MainContract.ErrorHandler {
    override fun handleGenericError(errorMsg: String) {
        uiResolver.showSnack(errorMsg)
    }
}
