package com.woocommerce.android.ui.woopos.home

import android.os.Parcelable
import androidx.annotation.StringRes
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
    data class ProductsInfoDialog(
        val header: Int = R.string.woopos_dialog_products_info_heading,
        val primaryMessage: Int = R.string.woopos_dialog_products_info_primary_message,
        val secondaryMessage: Int = R.string.woopos_dialog_products_info_secondary_message,
        val primaryButton: PrimaryButton = PrimaryButton(
            label = R.string.woopos_dialog_products_info_button_label,
        ),
        val isVisible: Boolean,
    ) : Parcelable {
        @Parcelize
        data class PrimaryButton(
            @StringRes val label: Int,
        ) : Parcelable
    }
}

@Parcelize
data object WooPosExitConfirmationDialog : Parcelable {
    @IgnoredOnParcel
    val title: Int = R.string.woopos_exit_dialog_confirmation_title

    @IgnoredOnParcel
    val message: Int = R.string.woopos_exit_dialog_confirmation_message

    @IgnoredOnParcel
    val confirmButton: Int = R.string.woopos_exit_dialog_confirmation_confirm_button
}
