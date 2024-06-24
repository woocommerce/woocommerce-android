package com.woocommerce.android.ui.woopos.home.totals

import android.os.Parcelable
import com.woocommerce.android.ui.woopos.common.composeui.component.snackbar.WooPosSnackbarState
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class WooPosTotalsState(
    open val orderId: Long? = null,
    open var orderSubtotalText: String,
    open var orderTaxText: String,
    open var orderTotalText: String,
) : Parcelable {
    data class Totals(
        override val orderId: Long? = null,
        override var orderSubtotalText: String,
        override var orderTaxText: String,
        override var orderTotalText: String,
        val snackbar: WooPosSnackbarState = WooPosSnackbarState.Hidden,
        val isLoading: Boolean,
        val isCollectPaymentButtonEnabled: Boolean,
    ) : WooPosTotalsState(orderId, orderSubtotalText, orderTaxText, orderTotalText)

    data class PaymentSuccess(
        override val orderId: Long? = null,
        override var orderSubtotalText: String,
        override var orderTaxText: String,
        override var orderTotalText: String,
    ) : WooPosTotalsState(orderId, orderSubtotalText, orderTaxText, orderTotalText)
}
