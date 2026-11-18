package com.woocommerce.android.ui.woopos.roles.pin

sealed class WooPosPINError : Exception() {
    data object InvalidFormat : WooPosPINError()
    data object AdminRequired : WooPosPINError()
    data object MatchesOtherRole : WooPosPINError()
    data object StorageFailure : WooPosPINError()
}
