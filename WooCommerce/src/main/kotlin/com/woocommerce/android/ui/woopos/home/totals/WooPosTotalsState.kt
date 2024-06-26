package com.woocommerce.android.ui.woopos.home.totals

import android.os.Parcelable
import com.woocommerce.android.ui.woopos.common.composeui.component.snackbar.WooPosSnackbarState
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class WooPosTotalsState : Parcelable {
    data object Loading : WooPosTotalsState()

    data class Totals(
        var orderSubtotalText: String,
        var orderTaxText: String,
        var orderTotalText: String,
        val snackbar: WooPosSnackbarState = WooPosSnackbarState.Hidden,
    ) : WooPosTotalsState()

    data object PaymentSuccess : WooPosTotalsState()
}
