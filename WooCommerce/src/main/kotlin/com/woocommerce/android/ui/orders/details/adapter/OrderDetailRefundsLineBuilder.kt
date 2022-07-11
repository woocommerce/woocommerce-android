package com.woocommerce.android.ui.orders.details.adapter

import com.woocommerce.android.R
import com.woocommerce.android.model.Refund
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class OrderDetailRefundsLineBuilder @Inject constructor(
    private val resourceProvider: ResourceProvider,
) {
    fun buildRefundLine(refund: Refund): String {
        val tokens = mutableListOf<String>()
        if (refund.items.isNotEmpty()) {
            val quantity = refund.items.sumOf { it.quantity }
            val productString = resourceProvider.getQuantityString(
                quantity = quantity,
                default = R.string.orderdetail_product_multiple,
                one = R.string.orderdetail_product
            )
            tokens.add("$quantity $productString")
        }
        if (refund.shippingLines.isNotEmpty()) {
            tokens.add(resourceProvider.getString(R.string.product_shipping))
        }
        if (refund.feeLines.isNotEmpty()) {
            tokens.add(resourceProvider.getString(R.string.orderdetail_payment_fees))
        }
        return tokens.joinToString(separator = ", ") { it.lowercase() }
    }
}
