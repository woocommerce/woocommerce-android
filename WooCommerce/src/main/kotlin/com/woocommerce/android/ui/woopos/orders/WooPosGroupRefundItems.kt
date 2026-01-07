package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.refunds.RefundRequestItem
import org.wordpress.android.fluxc.model.refunds.RefundRequestTax
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import javax.inject.Inject

class WooPosGroupRefundItems @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
) {
    operator fun invoke(
        refundableItems: List<WooPosRefundableItem>,
        order: Order
    ): List<RefundRequestItem> {
        return refundableItems
            .groupBy { it.orderItemId }
            .map { (orderItemId, items) ->
                val originalItem = requireNotNull(order.items.find { it.itemId == orderItemId }) {
                    "Order item with ID $orderItemId not found in order ${order.id}."
                }
                val refundQuantity = items.size

                RefundRequestItem(
                    itemId = orderItemId,
                    quantity = refundQuantity,
                    refundTotal = calculateRefundTotal(originalItem, refundQuantity),
                    refundTax = calculateRefundTaxes(originalItem, refundQuantity)
                )
            }
    }

    private fun calculateRefundTotal(
        originalItem: Order.Item,
        quantity: Int
    ): BigDecimal {
        return originalItem.price.multiply(quantity.toBigDecimal())
    }

    private fun calculateRefundTaxes(
        originalItem: Order.Item,
        quantity: Int
    ): List<RefundRequestTax> {
        check(originalItem.quantity > 0f) {
            "Order item ${originalItem.itemId} has invalid quantity ${originalItem.quantity}."
        }

        val refundQuantity = quantity.toBigDecimal()
        val numberOfDecimals = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyDecimalNumber
        checkNotNull(numberOfDecimals) {
            "Failed to get site settings in order to determine number of decimals for refund tex calculations."
        }
        return originalItem.taxes.map { tax: Order.LineTaxEntry ->
            // Calculate per-unit tax with high precision to preserve accuracy
            val perUnitTax = tax.taxAmount.divide(
                originalItem.quantity.toBigDecimal(),
                MathContext.DECIMAL128
            )

            // Apply rounding to the final refund (tax) total
            val refundTotal = refundQuantity
                .multiply(perUnitTax)
                .setScale(numberOfDecimals, RoundingMode.HALF_UP)

            RefundRequestTax(
                taxRateId = tax.rateId,
                refundTotal = refundTotal
            )
        }
    }
}
