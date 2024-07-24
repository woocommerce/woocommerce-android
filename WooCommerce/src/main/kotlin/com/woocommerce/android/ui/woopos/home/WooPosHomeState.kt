package com.woocommerce.android.ui.woopos.home

import androidx.annotation.StringRes

sealed class WooPosHomeState {
    sealed class Cart : WooPosHomeState() {
        data object Empty : Cart()
        data object NotEmpty : Cart()
    }

    sealed class Checkout : WooPosHomeState() {
        data object NotPaid : Checkout()
        data object Paid : Checkout()
    }

    data class ProductsInfoDialog(
        val shouldDisplayDialog: Boolean = false,
        @StringRes val header: Int,
        @StringRes val primaryMessage: Int,
        @StringRes val secondaryMessage: Int,
        @StringRes val secondaryMessageActionLabel: Int,
        val primaryButton: PrimaryButton,
    ) : WooPosHomeState() {
        data class PrimaryButton(
            @StringRes val label: Int,
        )
    }
}
