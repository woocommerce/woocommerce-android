package com.woocommerce.android.cardreader.receipts

data class ReceiptData(
    val staticTexts: ReceiptStaticTexts,
    val purchasedProducts: List<ReceiptLineItem>,
    val amount: Float,
    val currency: String,
    val receiptDate: String,
    val storeName: String?,
    val applicationPreferredName: String?,
    val dedicatedFileName: String?, //  Also known as “AID”.
    val cardInfo: CardInfo
)

data class CardInfo(
    val last4CardDigits: String,
    val brand: PaymentCardBrand
)

data class ReceiptLineItem(
    val title: String,
    val quantity: Int,
    val amount: Float
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
