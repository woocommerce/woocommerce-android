package com.woocommerce.android.ui.woopos.home.totals

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

@Parcelize
data class WooPosTotalsState(
    val orderId: Long?,
    val isCollectPaymentButtonEnabled: Boolean,
    val snackbarMessage: SnackbarMessage = SnackbarMessage.Hidden,
) : Parcelable

@Parcelize
sealed class SnackbarMessage: Parcelable {
    data class Triggered(@StringRes val message: Int) : SnackbarMessage()
    data object Hidden: SnackbarMessage()
}
