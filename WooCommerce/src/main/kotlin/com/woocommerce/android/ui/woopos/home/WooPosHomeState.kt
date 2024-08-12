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
            sealed class Visible : Cart() {
                @Parcelize
                data object Empty : Cart(), Parcelable
                @Parcelize
                data object NotEmpty : Cart(), Parcelable
            }

            @Parcelize
            data object Hidden : Cart(), Parcelable
        }

        @Parcelize
        sealed class Checkout : ScreenPositionState() {
            @Parcelize
            data object NotPaid : Checkout(), Parcelable
            @Parcelize
            data object Paid : Checkout(), Parcelable
        }
    }

    @Parcelize
    sealed class ProductsInfoDialog : Parcelable {
        @Parcelize
        data object Hidden : ProductsInfoDialog(), Parcelable

        @Parcelize
        data class Visible(
            @StringRes val header: Int,
            @StringRes val primaryMessage: Int,
            @StringRes val secondaryMessage: Int,
            val primaryButton: PrimaryButton,
        ) : ProductsInfoDialog(), Parcelable {
            @Parcelize
            data class PrimaryButton(
                @StringRes val label: Int,
            ) : Parcelable
        }
    }
}

@Parcelize
data object WooPosExitConfirmationDialog : Parcelable {
    @IgnoredOnParcel
    val title: Int = R.string.woopos_exit_confirmation_title
    @IgnoredOnParcel
    val message: Int = R.string.woopos_exit_confirmation_message
    @IgnoredOnParcel
    val confirmButton: Int = R.string.woopos_exit_confirmation_confirm_button
    @IgnoredOnParcel
    val dismissButton: Int = R.string.woopos_exit_confirmation_dismiss_button
}
