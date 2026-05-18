package com.woocommerce.android.aiassistant.safety

import com.woocommerce.android.aiassistant.R
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
        when (context.descriptor.name) {
            ORDERS_UPDATE -> orderUpdatePreview(
                arguments = context.request.arguments,
                currentValues = currentOrderValues(context.request.arguments),
            )
            ORDERS_BULK_UPDATE -> ordersBulkUpdatePreview(context.request.arguments)
            else -> error("Unsupported order confirmation preview: ${context.descriptor.name}")
        }

    private fun orderUpdatePreview(
        arguments: JsonObject,
        currentValues: Map<String, String>?,
    ): ConfirmationPreview {
        val id = arguments.longValue("id")
            ?: return ConfirmationPreview(string(R.string.ai_assistant_confirmation_order_update_generic))
        val status = arguments.stringValue("status")
        val fields = buildList {
            status?.let {
                add(
                    textField(
                        name = "status",
                        value = it,
                        label = R.string.ai_assistant_confirmation_field_status,
                        beforeValue = currentValues?.get("status"),
                    )
                )
            }
            if (arguments.containsKey("customer_note")) {
                add(
                    messageField(
                        name = "customer_note",
                        value = raw(arguments.stringValue("customer_note").orEmpty().customerNotePreviewValue()),
                        label = R.string.ai_assistant_confirmation_field_customer_note,
                    )
                )
            }
            arguments.stringValue("billing_email")?.let {
                add(
                    textField(
                        name = "billing_email",
                        value = it,
                        label = R.string.ai_assistant_confirmation_field_billing_email,
                        beforeValue = currentValues?.get("billing_email"),
                    )
                )
            }
        }

        return ConfirmationPreview(
            message = string(
                if (status.emailsCustomer()) {
                    R.string.ai_assistant_confirmation_order_update_summary
                } else {
                    R.string.ai_assistant_confirmation_order_update_title
                },
                raw(id.toString()),
            ),
            fields = fields,
        )
    }

    private fun ordersBulkUpdatePreview(arguments: JsonObject): ConfirmationPreview {
        val ids = arguments.longArrayValue("ids")
            ?: return ConfirmationPreview(string(R.string.ai_assistant_confirmation_orders_bulk_update_generic))
        val patch = arguments.objectValue("patch")
            ?: return ConfirmationPreview(string(R.string.ai_assistant_confirmation_orders_bulk_update_generic))
        val fields = buildList {
            patch.stringValue("status")?.let { status ->
                add(textField("status", status, R.string.ai_assistant_confirmation_field_status))
            }
            if (patch.containsKey("customer_note")) {
                add(
                    messageField(
                        name = "customer_note",
                        value = string(R.string.ai_assistant_confirmation_field_value_updated),
                        label = R.string.ai_assistant_confirmation_field_customer_note,
                    )
                )
            }
            patch.stringValue("billing_email")?.let {
                add(textField("billing_email", it, R.string.ai_assistant_confirmation_field_billing_email))
            }
        }
        val emailsCustomer = patch.stringValue("status").emailsCustomer()
        return ConfirmationPreview(
            message = quantity(
                quantity = ids.size,
                singular = if (emailsCustomer) {
                    R.string.ai_assistant_confirmation_orders_bulk_update_summary_single
                } else {
                    R.string.ai_assistant_confirmation_orders_bulk_update_title_single
                },
                multiple = if (emailsCustomer) {
                    R.string.ai_assistant_confirmation_orders_bulk_update_summary_multiple
                } else {
                    R.string.ai_assistant_confirmation_orders_bulk_update_title_multiple
                },
            ),
            fields = fields,
            isBulk = true,
            bulkEntries = ids.map { ConfirmationBulkEntry(it) },
        )
    }

    private suspend fun currentOrderValues(arguments: JsonObject): Map<String, String>? =
        arguments.longValue("id")
            ?.let { orderId -> ordersDataSource.getOrder(orderId).getOrNull() }
            ?.let { order ->
                mapOf(
                    "status" to order.status.removePrefix("wc-"),
                    "customer_note" to order.customerNote,
                    "billing_email" to order.billingEmail,
                )
            }

    private fun String.customerNotePreviewValue(): String =
        if (length > CUSTOMER_NOTE_PREVIEW_LIMIT) {
            "${take(CUSTOMER_NOTE_PREVIEW_LIMIT)}..."
        } else {
            this
        }

    private fun String?.emailsCustomer(): Boolean =
        this?.let { it in CUSTOMER_NOTIFYING_STATUSES } == true

    private companion object {
        const val ORDERS_UPDATE = "orders_update"
        const val ORDERS_BULK_UPDATE = "orders_bulk_update"
        const val CUSTOMER_NOTE_PREVIEW_LIMIT = 160
        val SUPPORTED_TOOL_NAMES = setOf(ORDERS_UPDATE, ORDERS_BULK_UPDATE)
        val CUSTOMER_NOTIFYING_STATUSES = setOf(
            "processing",
            "completed",
            "cancelled",
            "refunded",
            "on-hold",
        )
    }
}
