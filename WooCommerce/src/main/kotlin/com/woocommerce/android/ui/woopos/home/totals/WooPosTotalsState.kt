package com.woocommerce.android.ui.woopos.home.totals

import android.os.Parcelable
import com.woocommerce.android.ui.woopos.common.composeui.component.snackbar.WooPosSnackbarState
import kotlinx.parcelize.Parcelize

@Parcelize
data class WooPosTotalsState(
    val orderId: Long? = null,
    val isCollectPaymentButtonEnabled: Boolean,
    var orderSubtotalText: String,
    var orderTaxText: String,
    var orderTotalText: String,
    val snackbar: WooPosSnackbarState = WooPosSnackbarState.Hidden,
    val isLoading: Boolean,
) : Parcelable
