package com.woocommerce.android.ui.woopos.home.totals

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WooPosTotalsState(
    val orderId: Long?, // TODO: remove. Ultimately we don't need to display order id anywhere
    val isCollectPaymentButtonEnabled: Boolean,
) : Parcelable
