package com.woocommerce.android.ui.login.qrcode

import kotlin.contracts.ExperimentalContracts

object ValidateScannedValue {

    private const val MAGIC_LOGIN_ACTION = "magic-login"
    private const val MAGIC_LOGIN_SCHEME = "woocommerce"

    @OptIn(ExperimentalContracts::class)
    fun validate(scannedRawValue: String?): Boolean {
        return scannedRawValue != null &&
            scannedRawValue.contains(MAGIC_LOGIN_ACTION) &&
            scannedRawValue.contains(MAGIC_LOGIN_SCHEME)
    }
}
