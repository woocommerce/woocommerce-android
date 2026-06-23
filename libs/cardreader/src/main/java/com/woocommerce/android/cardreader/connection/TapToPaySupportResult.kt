package com.woocommerce.android.cardreader.connection

sealed class TapToPaySupportResult {
    object Supported : TapToPaySupportResult()
    data class NotSupported(val reason: String) : TapToPaySupportResult()
    object TerminalNotInitialized : TapToPaySupportResult()
}
