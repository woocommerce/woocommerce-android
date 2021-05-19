package com.woocommerce.android.cardreader.receipts

data class ReceiptData(
    val purchasedProducts: List<ReceiptLineItem>,
    val amount: Int,
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
    val amount: Int
)
