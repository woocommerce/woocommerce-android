package com.woocommerce.android.ui.woopos.roles

sealed class WooPosPINError : Exception() {
    data object InvalidFormat : WooPosPINError()
    data object AdminRequired : WooPosPINError()
    data object MatchesOtherRole : WooPosPINError()
    data object StorageFailure : WooPosPINError()
}
