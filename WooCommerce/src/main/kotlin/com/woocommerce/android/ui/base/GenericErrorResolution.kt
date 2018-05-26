package com.woocommerce.android.ui.base

import com.woocommerce.android.ui.main.MainUIMessageResolver
import javax.inject.Inject

open class GenericErrorResolution @Inject constructor(val uiResolver: MainUIMessageResolver) {
    fun handleGenericError(errorMsg: String) {
        uiResolver.showSnack(errorMsg)
    }
}
