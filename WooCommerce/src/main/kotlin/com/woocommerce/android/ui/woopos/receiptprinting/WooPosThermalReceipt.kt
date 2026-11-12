package com.woocommerce.android.ui.woopos.receiptprinting

data class WooPosThermalReceipt(
    val storeName: String,
    val storeAddress: String,
    val storePhone: String,
    val storeEmail: String,
    val datePaid: String,
    val orderNumber: String,
    val lineItems: List<LineItem>,
    val totals: Totals,
    val orderNote: String,
    val card: Card?,
    val returnsPolicy: String,
) {
    data class LineItem(
        val title: String,
        val attributes: String,
        val quantity: String,
        val amount: String,
    )

    data class Totals(
        val subtotal: String,
        val discount: Discount?,
        val fees: String?,
        val shipping: String?,
        val taxes: String?,
        val amountPaid: String,
    )

    data class Discount(
        val couponCodes: List<String>,
        val amount: String,
    )

    data class Card(
        val brand: String,
        val last4: String,
    )

    data class Labels(
        val datePaid: String,
        val orderNumber: String,
        val paymentMethod: String,
        val subtotal: String,
        val discount: String,
        val fees: String,
        val shipping: String,
        val taxes: String,
        val amountPaid: String,
        val notes: String,
    )
}
