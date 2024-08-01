package com.woocommerce.android.ui.woopos.home

import androidx.annotation.StringRes
import android.os.Parcelable
import com.woocommerce.android.R
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class WooPosHomeState(
    val screenPositionState: ScreenPositionState,
    val productsInfoDialog: ProductsInfoDialog,
    val exitConfirmationDialog: WooPosExitConfirmationDialog? = null,
) : Parcelable {
    @Parcelize
    sealed class ScreenPositionState : Parcelable {
        @Parcelize
        sealed class Cart : ScreenPositionState() {
            @Parcelize
            sealed class Visible : Cart() {
                @Parcelize
                data object Empty : Cart()
                @Parcelize
                data object NotEmpty : Cart()
            }

            @Parcelize
            data object Hidden : Cart()
        }

        @Parcelize
        sealed class Checkout : ScreenPositionState() {
            @Parcelize
            data object NotPaid : Checkout()
            @Parcelize
            data object Paid : Checkout()
        }
    }

    @Parcelize
    sealed class ProductsInfoDialog: Parcelable {
        data object Hidden : ProductsInfoDialog()

        data class Visible(
            @StringRes val header: Int,
            @StringRes val primaryMessage: Int,
            @StringRes val secondaryMessage: Int,
            val primaryButton: PrimaryButton,
        ) : ProductsInfoDialog() {
            @Parcelize
            data class PrimaryButton(
                @StringRes val label: Int,
            ): Parcelable
        }
    }
}

@Parcelize
data object WooPosExitConfirmationDialog: Parcelable {
    @IgnoredOnParcel
    val title: Int = R.string.woopos_exit_confirmation_title
    @IgnoredOnParcel
    val message: Int = R.string.woopos_exit_confirmation_message
    @IgnoredOnParcel
    val confirmButton: Int = R.string.woopos_exit_confirmation_confirm_button
    @IgnoredOnParcel
    val dismissButton: Int = R.string.woopos_exit_confirmation_dismiss_button
}
