package com.woocommerce.android.ui.login.qrcode

object ValidateScannedValue {

    private const val MAGIC_LOGIN_ACTION = "magic-login"
    private const val MAGIC_LOGIN_SCHEME = "woocommerce"

    fun validate(scannedRawValue: String?): Boolean {
        return scannedRawValue != null &&
            scannedRawValue.contains(MAGIC_LOGIN_ACTION) &&
            scannedRawValue.contains(MAGIC_LOGIN_SCHEME)
    }
}
