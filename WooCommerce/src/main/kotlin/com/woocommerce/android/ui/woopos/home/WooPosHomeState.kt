package com.woocommerce.android.ui.woopos.home

import android.os.Parcelable
import com.woocommerce.android.R
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class WooPosHomeState(
    val screenPositionState: ScreenPositionState,
    val dialogState: DialogState = DialogState.Hidden,
) : Parcelable {
    @Parcelize
    sealed class ScreenPositionState : Parcelable {
        @Parcelize
        data object Products : ScreenPositionState()

        @Parcelize
        data object Cart : ScreenPositionState()

        @Parcelize
        sealed class Checkout : ScreenPositionState() {
            @Parcelize
            data object CartWithTotals : Checkout()

            @Parcelize
            data object FullScreenTotals : Checkout()
        }
    }

    @Parcelize
    sealed class DialogState : Parcelable {
        @Parcelize
        data object Hidden : DialogState()

        @Parcelize
        data object ScanningSetupDialog : DialogState()

        @Parcelize
        data object ExitConfirmationDialog : DialogState() {
            @IgnoredOnParcel
            val title: Int = R.string.woopos_exit_dialog_confirmation_title

            @IgnoredOnParcel
            val message: Int = R.string.woopos_exit_dialog_confirmation_message

            @IgnoredOnParcel
            val confirmButton: Int = R.string.woopos_exit_dialog_confirmation_confirm_button
        }

        @Parcelize
        data object CardReaderConnectionDialog : DialogState()
    }
}
