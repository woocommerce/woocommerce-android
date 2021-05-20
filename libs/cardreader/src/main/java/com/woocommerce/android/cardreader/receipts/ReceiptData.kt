package com.woocommerce.android.cardreader.receipts

data class ReceiptData(
    val staticTexts: ReceiptStaticTexts,
    val purchasedProducts: List<ReceiptLineItem>,
    val storeName: String?,
    val paymentInfo: PaymentInfo
)

data class PaymentInfo(
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
    val quantity: Int,
    val itemsTotalAmount: Float
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
