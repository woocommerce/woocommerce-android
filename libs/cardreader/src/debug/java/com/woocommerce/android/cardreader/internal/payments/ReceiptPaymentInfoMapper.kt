package com.woocommerce.android.cardreader.internal.payments

import com.stripe.stripeterminal.model.external.PaymentIntent
import com.woocommerce.android.cardreader.receipts.CardInfo
import com.woocommerce.android.cardreader.receipts.PaymentCardBrand
import com.woocommerce.android.cardreader.receipts.ReceiptPaymentInfo
import kotlin.math.pow

class ReceiptPaymentInfoMapper {
    @Throws(IllegalArgumentException::class)
    fun mapPaymentIntentToPaymentInfo(paymentIntent: PaymentIntent): ReceiptPaymentInfo {
        val charge = paymentIntent.getCharges().getOrNull(0)
            ?: throw IllegalArgumentException("PaymentIntent does not contain any Charges")
        val chargedTotalAmount = charge.amount / 10f.pow(USD_TO_CENTS_DECIMAL_PLACES)
        val receiptDate = charge.created
        val currency = charge.currency.orEmpty()

        val cardPresentDetails = charge.paymentMethodDetails?.cardPresentDetails
            ?: throw IllegalArgumentException("PaymentIntent does not contain CardPresentDetails")
        val cardBrand = mapToPaymentCardBrand(cardPresentDetails.brand)
        val last4CardDigits = cardPresentDetails.last4.orEmpty()

        val receiptDetails = cardPresentDetails.receiptDetails
            ?: throw IllegalArgumentException("PaymentIntent does not contain ReceiptDetails")
        val applicationPreferredName = receiptDetails.applicationPreferredName
            ?: throw IllegalArgumentException("PaymentIntent does not contain applicationPreferredName")
        val dedicatedFileName = receiptDetails.dedicatedFileName
            ?: throw IllegalArgumentException("PaymentIntent does not contain dedicatedFileName")

        return ReceiptPaymentInfo(
            chargedAmount = chargedTotalAmount,
            currency,
            receiptDate,
            applicationPreferredName,
            dedicatedFileName,
            cardInfo = CardInfo(last4CardDigits, cardBrand)
        )
    }

    private fun mapToPaymentCardBrand(brand: String?): PaymentCardBrand {
        return PaymentCardBrand.values().firstOrNull { it.key.equals(brand, ignoreCase = true) }
            ?: PaymentCardBrand.UNKNOWN
    }
}
