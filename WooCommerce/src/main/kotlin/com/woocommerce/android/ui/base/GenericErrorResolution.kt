package com.woocommerce.android.ui.base

import com.woocommerce.android.ui.main.UIMessageResolver
import javax.inject.Inject

open class GenericErrorResolution @Inject constructor(val uiResolver: UIMessageResolver) {
    fun handleGenericError(errorMsg: String) {
        uiResolver.showSnack(errorMsg)
    }
}
