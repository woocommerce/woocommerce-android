package com.woocommerce.android.cardreader.payments

import java.math.BigDecimal

data class PaymentInfo(
    val paymentDescription: String,
    val statementDescriptor: StatementDescriptor,
    val orderId: Long,
    val amount: BigDecimal,
    val currency: String,
    val customerEmail: String?,
    val isPluginCanSendReceipt: Boolean,
    val customerName: String?,
    val storeName: String?,
    val siteUrl: String?,
    val orderKey: String?,
    val feeAmount: Long?,
    val channel: PaymentChannel?,
    val cardPresentCaptureMethod: CardPresentCaptureMethod? = null,
    val terminalPaymentPreparation: TerminalPaymentPreparation = TerminalPaymentPreparation.NONE,
    internal val countryCode: String? = null,
) {
    sealed class PaymentChannel(val value: String) {
        data object StoreManager : PaymentChannel("mobile_store_management")
        data object Pos : PaymentChannel("mobile_pos")
    }

    enum class CardPresentCaptureMethod {
        MANUAL_PREFERRED,
    }

    enum class TerminalPaymentPreparation {
        NONE,
        CANADA_INTERAC,
        AUSTRALIA_CARD_PRESENT,
    }
}
