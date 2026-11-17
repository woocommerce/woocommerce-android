package com.woocommerce.android.ui.woopos.receiptprinting

data class WooPosThermalReceipt(
    val storeName: String,
    val storeAddress: String?,
    val storePhone: String?,
    val storeEmail: String?,
    val datePaid: String,
    val orderNumber: String,
    val paymentMethod: String?,
    val lineItems: List<Row>,
    val totals: List<Row>,
    val orderNote: String?,
    val refundPolicy: String?,
) {
    data class Row(
        val label: String,
        val amount: String,
    )

    data class Labels(
        val datePaid: String,
        val paymentMethod: String,
        val notes: String,
    )
}
