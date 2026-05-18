package com.woocommerce.android.cardreader.remote

import com.woocommerce.android.cardreader.payments.PaymentInfo
import java.math.BigDecimal

internal sealed class CardReaderRemoteMessage {
    abstract val requestId: String

    data class ConnectRequest(
        override val requestId: String,
        val connectionToken: String,
        val locationId: String,
    ) : CardReaderRemoteMessage()

    data class ConnectAck(
        override val requestId: String,
        val readerSerial: String?,
    ) : CardReaderRemoteMessage()

    data class CollectPaymentRequest(
        override val requestId: String,
        val paymentDescription: String,
        val statementDescriptorRaw: String?,
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
        val countryCode: String?,
        val cardPresentCaptureMethod: PaymentInfo.CardPresentCaptureMethod?,
        val terminalPaymentPreparation: PaymentInfo.TerminalPaymentPreparation?,
    ) : CardReaderRemoteMessage()

    data class PaymentIntentResult(
        override val requestId: String,
        val paymentIntentId: String,
        val status: String,
    ) : CardReaderRemoteMessage()

    data class ErrorMessage(
        override val requestId: String,
        val code: String,
        val description: String,
    ) : CardReaderRemoteMessage()

    data class Ping(
        override val requestId: String,
    ) : CardReaderRemoteMessage()
}
