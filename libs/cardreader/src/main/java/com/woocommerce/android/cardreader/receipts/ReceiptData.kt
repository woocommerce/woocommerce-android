package com.woocommerce.android.cardreader.receipts

import java.math.BigDecimal

data class ReceiptData(
    val staticTexts: ReceiptStaticTexts,
    val purchasedProducts: List<ReceiptLineItem>,
    val storeName: String?,
    val receiptPaymentInfo: ReceiptPaymentInfo
)

data class ReceiptPaymentInfo(
    val chargedAmount: Float,
    val currency: String,
    val receiptDate: Long,
    val applicationPreferredName: String,
    val dedicatedFileName: String, //  Also known as “AID”.
    val cardInfo: CardInfo
)

data class CardInfo(
    val last4CardDigits: String,
    val brand: PaymentCardBrand
)

data class ReceiptLineItem(
    val title: String,
    val quantity: Float,
    val itemsTotalAmount: BigDecimal
)

data class ReceiptStaticTexts(
    val applicationName: String,
    val receiptFromFormat: String,
    val receiptTitle: String,
    val amountPaidSectionTitle: String,
    val datePaidSectionTitle: String,
    val paymentMethodSectionTitle: String,
    val summarySectionTitle: String,
    val aid: String
)
