package com.woocommerce.android.aiassistant.safety

import com.woocommerce.android.aiassistant.tools.orders.AIOrdersDataSource
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject

internal class OrdersConfirmationPreviewProvider @Inject constructor(
    private val ordersDataSource: AIOrdersDataSource,
) : ConfirmationPreviewProvider {
    override val key: String = "woocommerce_orders"
    override val priority: Int = 100

    override fun canPreview(context: ConfirmationPreviewContext): Boolean =
        context.descriptor.name in SUPPORTED_TOOL_NAMES

    override suspend fun buildPreview(context: ConfirmationPreviewContext): ConfirmationPreview =
        when (context.request.toolName) {
            ORDERS_UPDATE -> WooCommerceConfirmationPreviewFormatters.orderUpdatePreview(
                arguments = context.request.arguments,
                currentValues = currentOrderValues(context.request.arguments),
            )
            ORDERS_BULK_UPDATE -> WooCommerceConfirmationPreviewFormatters.ordersBulkUpdatePreview(
                context.request.arguments
            )
            else -> error("Unsupported order confirmation preview: ${context.request.toolName}")
        }

    private suspend fun currentOrderValues(arguments: JsonObject): Map<String, String>? =
        WooCommerceConfirmationPreviewFormatters.run { arguments.longValue("id") }
            ?.let { orderId -> ordersDataSource.getOrder(orderId).getOrNull() }
            ?.let { order ->
                mapOf(
                    "status" to order.status.removePrefix("wc-"),
                    "customer_note" to order.customerNote,
                    "billing_email" to order.billingEmail,
                )
            }

    private companion object {
        const val ORDERS_UPDATE = "orders_update"
        const val ORDERS_BULK_UPDATE = "orders_bulk_update"
        val SUPPORTED_TOOL_NAMES = setOf(ORDERS_UPDATE, ORDERS_BULK_UPDATE)
    }
}
