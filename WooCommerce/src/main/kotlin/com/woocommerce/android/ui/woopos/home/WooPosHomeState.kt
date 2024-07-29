package com.woocommerce.android.ui.woopos.home

import androidx.annotation.StringRes
import com.woocommerce.android.R

data class WooPosHomeState(
    val screenPositionState: ScreenPositionState,
    val productsInfoDialog: ProductsInfoDialog,
    val exitConfirmationDialog: WooPosExitConfirmationDialog? = null,
) {
    sealed class ScreenPositionState {
        sealed class Cart : ScreenPositionState() {
            sealed class Visible : Cart() {
                data object Empty : Cart()
                data object NotEmpty : Cart()
            }

            data object Hidden : Cart()
        }

        sealed class Checkout : ScreenPositionState() {
            data object NotPaid : Checkout()
            data object Paid : Checkout()
        }
    }

    sealed class ProductsInfoDialog {
        data object Hidden : ProductsInfoDialog()

        data class Visible(
            @StringRes val header: Int,
            @StringRes val primaryMessage: Int,
            @StringRes val secondaryMessage: Int,
            val primaryButton: PrimaryButton,
        ) : ProductsInfoDialog() {
            data class PrimaryButton(
                @StringRes val label: Int,
            )
        }
    }
}

data object WooPosExitConfirmationDialog {
    val title: Int = R.string.woopos_exit_confirmation_title
    val message: Int = R.string.woopos_exit_confirmation_message
    val confirmButton: Int = R.string.woopos_exit_confirmation_confirm_button
    val dismissButton: Int = R.string.woopos_exit_confirmation_dismiss_button
}
