package com.woocommerce.android.util.receipts

import com.woocommerce.android.R
import com.woocommerce.android.cardreader.receipts.ReceiptLineItem
import com.woocommerce.android.extensions.roundError
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.fluxc.model.WCOrderModel
import javax.inject.Inject

class ReceiptLineItemMapper @Inject constructor(private val resourceProvider: ResourceProvider) {
    fun createReceiptLineItems(order: WCOrderModel): List<ReceiptLineItem> =
        getProducts(order) + getDiscounts(order) + getFees(order) + getShipping(order) + getTaxes(order)

    private fun getProducts(order: WCOrderModel) = order.getLineItemList()
        .mapNotNull { line ->
            line.total?.toBigDecimalOrNull()?.roundError()?.let { total ->
                ReceiptLineItem(
                    title = line.name.orEmpty(),
                    quantity = line.quantity ?: 1f,
                    itemsTotalAmount = total
                )
            }
        }

    private fun getDiscounts(order: WCOrderModel) =
        order.discountTotal.toBigDecimalOrNull()?.roundError()?.let { total ->
            listOf(
                ReceiptLineItem(
                    title = resourceProvider.getString(R.string.discount),
                    quantity = 1f,
                    itemsTotalAmount = total
                )
            )
        } ?: listOf()

    private fun getFees(order: WCOrderModel) = order.getFeeLineList()
        .mapNotNull { line ->
            line.total?.toBigDecimalOrNull()?.roundError()?.let { total ->
                ReceiptLineItem(
                    title = line.name.orEmpty(),
                    quantity = 1f,
                    itemsTotalAmount = total
                )
            }
        }

    private fun getShipping(order: WCOrderModel) = order.getShippingLineList()
        .mapNotNull { line ->
            line.total?.toBigDecimalOrNull()?.roundError()?.let { total ->
                ReceiptLineItem(
                    title = line.methodTitle.orEmpty(),
                    quantity = 1f,
                    itemsTotalAmount = total
                )
            }
        }

    private fun getTaxes(order: WCOrderModel) =
        order.totalTax.toBigDecimalOrNull()?.roundError()?.let { total ->
            listOf(
                ReceiptLineItem(
                    title = resourceProvider.getString(R.string.taxes),
                    quantity = 1f,
                    itemsTotalAmount = total
                )
            )
        } ?: listOf()
}
