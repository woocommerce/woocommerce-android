package com.woocommerce.android.ui.woopos.orders

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

@Immutable
sealed class WooPosRefundState {
    @Immutable
    data object Loading : WooPosRefundState()

    @Immutable
    data class Content(
        val orderId: Long,
        val orderNumber: String,
        val currency: String,
        val refundableItems: List<WooPosRefundableItem>,
        val itemsLabel: String,
        val subtotal: BigDecimal,
        val taxes: BigDecimal,
        val total: BigDecimal
    ) : WooPosRefundState()

    @Immutable
    data class Error(
        val message: String
    ) : WooPosRefundState()

    @Immutable
    data object NoRefundableItems : WooPosRefundState()
}
