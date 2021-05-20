package com.woocommerce.android.cardreader.internal.payments

import com.stripe.stripeterminal.model.external.PaymentIntent
import com.woocommerce.android.cardreader.receipts.CardInfo
import com.woocommerce.android.cardreader.receipts.PaymentCardBrand
import com.woocommerce.android.cardreader.receipts.PaymentInfo
import kotlin.math.pow

class PaymentInfoMapper {
    @Throws(IllegalStateException::class)
    fun mapPaymentIntentToPaymentInfo(paymentIntent: PaymentIntent): PaymentInfo {
        val charge = paymentIntent.getCharges().getOrNull(0)
            ?: throw IllegalStateException("PaymentIntent does not contain any Charges")
        val chargedTotalAmount = charge.amount.toFloat() / 10f.pow(USD_TO_CENTS_DECIMAL_PLACES)
        val receiptDate = charge.created
        val currency = charge.currency
            ?: throw IllegalStateException("PaymentIntent does not contain currency")

        val cardPresentDetails = charge.paymentMethodDetails?.cardPresentDetails
            ?: throw IllegalStateException("PaymentIntent does not contain CardPresentDetails")
        val cardBrand = mapToPaymentCardBrand(cardPresentDetails.brand)
        val last4CardDigits = cardPresentDetails.last4.orEmpty()

        val receiptDetails = cardPresentDetails.receiptDetails
            ?: throw IllegalStateException("PaymentIntent does not contain ReceiptDetails")
        val applicationPreferredName = receiptDetails.applicationPreferredName
            ?: throw IllegalStateException("PaymentIntent does not contain applicationPreferredName")
        val dedicatedFileName = receiptDetails.dedicatedFileName
            ?: throw IllegalStateException("PaymentIntent does not contain dedicatedFileName")

        return PaymentInfo(
            chargedAmount = chargedTotalAmount,
            currency,
            receiptDate,
            applicationPreferredName,
            dedicatedFileName,
            cardInfo = CardInfo(last4CardDigits, cardBrand)
        )
    }

    private fun mapToPaymentCardBrand(brand: String?): PaymentCardBrand {
        return PaymentCardBrand.values().firstOrNull { it.key == brand } ?: PaymentCardBrand.UNKNOWN
    }
}
