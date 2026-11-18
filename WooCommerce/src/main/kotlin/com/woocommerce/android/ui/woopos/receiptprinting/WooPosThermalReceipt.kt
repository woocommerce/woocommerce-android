package com.woocommerce.android.ui.woopos.receiptprinting

data class WooPosThermalReceipt(
    val storeName: String,
    val storeAddress: String?,
    val storePhone: String?,
    val storeEmail: String?,
    val datePaid: LabeledText,
    val orderNumber: String,
    val paymentMethod: LabeledText?,
    val lineItems: List<Row>,
    val totals: List<Row>,
    val orderNote: LabeledText?,
    val refundPolicy: String?,
) {
    data class Row(
        val label: String,
        val amount: String,
    )

    data class LabeledText(
        val label: String,
        val text: String,
    )
}
