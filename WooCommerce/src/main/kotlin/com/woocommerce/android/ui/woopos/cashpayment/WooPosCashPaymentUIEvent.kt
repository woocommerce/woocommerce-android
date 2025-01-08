package com.woocommerce.android.ui.woopos.cashpayment

import java.math.BigDecimal

sealed class WooPosCashPaymentUIEvent {
    object CompleteOrderClicked : WooPosCashPaymentUIEvent()
    data class AmountChanged(val newAmount: BigDecimal?) : WooPosCashPaymentUIEvent()
}
