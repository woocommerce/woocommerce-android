package com.woocommerce.android.aiassistant.safety

import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooCommerceConfirmationPreviewBuilderTest {
    private val builder = WooCommerceConfirmationPreviewBuilder()

    @Test
    fun `given order status update emails customer, when preview is built, then resource message is included`() {
        val call = toolCall(
            name = "orders_update",
            arguments = buildJsonObject {
                put("id", 42)
                put("status", "processing")
            },
        )

        val preview = builder.build(call)

        assertThat(preview.message).isEqualTo(
            string(
                R.string.ai_assistant_confirmation_order_set_status_emails_customer,
                raw("42"),
                raw("processing"),
            )
        )
        assertThat(preview.fields).containsExactly(
            ConfirmationPreviewField(
                name = "status",
                value = raw("processing"),
                label = label(R.string.ai_assistant_confirmation_field_status),
            ),
        )
    }

    @Test
    fun `given order update with large id, when preview is built, then id is preserved`() {
        val call = toolCall(
            name = "orders_update",
            arguments = buildJsonObject {
                put("id", 3_000_000_000L)
                put("status", "pending")
            },
        )

        val preview = builder.build(call)

        assertThat(preview.message).isEqualTo(
            string(R.string.ai_assistant_confirmation_order_set_status, raw("3000000000"), raw("pending"))
        )
    }

    @Test
    fun `given single order update has unsupported fields, when preview is built, then they are ignored`() {
        val call = toolCall(
            name = "orders_update",
            arguments = buildJsonObject {
                put("id", 42)
                put("status", "pending")
                put("customer_note", "Thanks")
                put("billing_email", "buyer@example.com")
            },
        )

        val preview = builder.build(call)

        assertThat(preview.message).isEqualTo(
            string(R.string.ai_assistant_confirmation_order_set_status, raw("42"), raw("pending"))
        )
        assertThat(preview.fields).containsExactly(
            ConfirmationPreviewField(
                name = "status",
                value = raw("pending"),
                label = label(R.string.ai_assistant_confirmation_field_status),
            ),
        )
    }

    @Test
    fun `given bulk order status update emails customers, when preview is built, then nested summary is included`() {
        val call = toolCall(
            name = "orders_bulk_update",
            arguments = buildJsonObject {
                put("ids", JsonArray((1..5).map { JsonPrimitive(it) }))
                put("patch", buildJsonObject { put("status", "completed") })
            },
        )

        val preview = builder.build(call)

        val summary = string(
            R.string.ai_assistant_confirmation_change_summary_status_emails_customers,
            raw("completed"),
        )
        assertThat(preview.message).isEqualTo(
            quantity(
                quantity = 5,
                singular = R.string.ai_assistant_confirmation_orders_bulk_update_summary_single,
                multiple = R.string.ai_assistant_confirmation_orders_bulk_update_summary_multiple,
                summary,
            )
        )
        assertThat(preview.fields).containsExactly(
            ConfirmationPreviewField(
                name = "status",
                value = raw("completed"),
                label = label(R.string.ai_assistant_confirmation_field_status),
            ),
        )
    }

    @Test
    fun `given one order bulk status update emails customer, when preview is built, then singular impact is used`() {
        val call = toolCall(
            name = "orders_bulk_update",
            arguments = buildJsonObject {
                put("ids", JsonArray(listOf(JsonPrimitive(42))))
                put("patch", buildJsonObject { put("status", "completed") })
            },
        )

        val preview = builder.build(call)

        val summary = string(
            R.string.ai_assistant_confirmation_change_summary_status_emails_customer,
            raw("completed"),
        )
        assertThat(preview.message).isEqualTo(
            quantity(
                quantity = 1,
                singular = R.string.ai_assistant_confirmation_orders_bulk_update_summary_single,
                multiple = R.string.ai_assistant_confirmation_orders_bulk_update_summary_multiple,
                summary,
            )
        )
    }

    @Test
    fun `given product price and stock update, when preview is built, then exact changes are included`() {
        val call = toolCall(
            name = "products_update",
            arguments = buildJsonObject {
                put("id", 7)
                put("regular_price", "24.99")
                put("stock_quantity", 100)
            },
        )

        val preview = builder.build(call)

        val summary = string(
            R.string.ai_assistant_confirmation_message_list_separator,
            string(R.string.ai_assistant_confirmation_change_summary_regular_price, raw("24.99")),
            string(R.string.ai_assistant_confirmation_change_summary_stock_quantity, raw("100")),
        )
        assertThat(preview.message).isEqualTo(
            string(R.string.ai_assistant_confirmation_product_update_summary, raw("7"), summary)
        )
        assertThat(preview.fields).containsExactly(
            ConfirmationPreviewField(
                name = "regular_price",
                value = raw("24.99"),
                label = label(R.string.ai_assistant_confirmation_field_regular_price),
            ),
            ConfirmationPreviewField(
                name = "stock_quantity",
                value = raw("100"),
                label = label(R.string.ai_assistant_confirmation_field_stock_quantity),
            ),
        )
    }

    @Test
    fun `given product update has unsupported stock status, when preview is built, then it is ignored`() {
        val call = toolCall(
            name = "products_update",
            arguments = buildJsonObject {
                put("id", 7)
                put("regular_price", "24.99")
                put("stock_status", "instock")
            },
        )

        val preview = builder.build(call)

        assertThat(preview.message).isEqualTo(
            string(
                R.string.ai_assistant_confirmation_product_update_summary,
                raw("7"),
                string(R.string.ai_assistant_confirmation_change_summary_regular_price, raw("24.99")),
            )
        )
        assertThat(preview.fields).containsExactly(
            ConfirmationPreviewField(
                name = "regular_price",
                value = raw("24.99"),
                label = label(R.string.ai_assistant_confirmation_field_regular_price),
            ),
        )
    }

    @Test
    fun `given bulk product update, when preview is built, then count and fields are included`() {
        val call = toolCall(
            name = "products_bulk_update",
            arguments = buildJsonObject {
                put("ids", JsonArray(listOf(JsonPrimitive(7), JsonPrimitive(8))))
                put(
                    "patch",
                    buildJsonObject {
                        put("regular_price", "19.99")
                        put("status", "draft")
                    },
                )
            },
        )

        val preview = builder.build(call)

        val summary = string(
            R.string.ai_assistant_confirmation_message_list_separator,
            string(R.string.ai_assistant_confirmation_change_summary_regular_price, raw("19.99")),
            string(R.string.ai_assistant_confirmation_change_summary_status, raw("draft")),
        )
        assertThat(preview.message).isEqualTo(
            quantity(
                quantity = 2,
                singular = R.string.ai_assistant_confirmation_products_bulk_update_summary_single,
                multiple = R.string.ai_assistant_confirmation_products_bulk_update_summary_multiple,
                summary,
            )
        )
        assertThat(preview.fields).containsExactly(
            ConfirmationPreviewField(
                name = "regular_price",
                value = raw("19.99"),
                label = label(R.string.ai_assistant_confirmation_field_regular_price),
            ),
            ConfirmationPreviewField(
                name = "status",
                value = raw("draft"),
                label = label(R.string.ai_assistant_confirmation_field_status),
            ),
        )
    }

    @Test
    @Suppress("LongMethod")
    fun `given product variation update, when preview is built, then product and variation ids are included`() {
        val call = toolCall(
            name = "product_variations_update",
            arguments = buildJsonObject {
                put("product_id", 7)
                put("id", 8)
                put("regular_price", "19.99")
                put("sale_price", "")
                put("stock_quantity", 3)
                put("stock_status", "instock")
                put("sku", "VAR-8")
                put("status", "publish")
            },
        )

        val preview = builder.build(call)

        val summary = listOf(
            string(R.string.ai_assistant_confirmation_change_summary_regular_price, raw("19.99")),
            string(
                R.string.ai_assistant_confirmation_change_summary_sale_price,
                string(R.string.ai_assistant_confirmation_field_value_off),
            ),
            string(R.string.ai_assistant_confirmation_change_summary_stock_quantity, raw("3")),
            string(R.string.ai_assistant_confirmation_change_summary_stock_status, raw("instock")),
            string(R.string.ai_assistant_confirmation_change_summary_status, raw("publish")),
            string(R.string.ai_assistant_confirmation_change_summary_sku, raw("VAR-8")),
        ).toLocalizedList()
        assertThat(preview.message).isEqualTo(
            string(
                R.string.ai_assistant_confirmation_product_variation_update_summary,
                raw("8"),
                raw("7"),
                summary,
            )
        )
        assertThat(preview.fields).containsExactly(
            ConfirmationPreviewField(
                name = "regular_price",
                value = raw("19.99"),
                label = label(R.string.ai_assistant_confirmation_field_regular_price),
            ),
            ConfirmationPreviewField(
                name = "sale_price",
                value = string(R.string.ai_assistant_confirmation_field_value_off),
                label = label(R.string.ai_assistant_confirmation_field_sale_price),
            ),
            ConfirmationPreviewField(
                name = "stock_quantity",
                value = raw("3"),
                label = label(R.string.ai_assistant_confirmation_field_stock_quantity),
            ),
            ConfirmationPreviewField(
                name = "stock_status",
                value = raw("instock"),
                label = label(R.string.ai_assistant_confirmation_field_stock_status),
            ),
            ConfirmationPreviewField(
                name = "status",
                value = raw("publish"),
                label = label(R.string.ai_assistant_confirmation_field_status),
            ),
            ConfirmationPreviewField(
                name = "sku",
                value = raw("VAR-8"),
                label = label(R.string.ai_assistant_confirmation_field_sku),
            ),
        )
    }

    @Test
    fun `given unknown tool, when preview is built, then raw arguments are omitted`() {
        val call = toolCall(
            name = "custom_tool",
            arguments = buildJsonObject { put("id", 1) },
        )

        val preview = builder.build(call)

        assertThat(preview.message).isEqualTo(
            string(R.string.ai_assistant_confirmation_generic_tool_call, raw("custom_tool"))
        )
        assertThat(preview.fields).isEmpty()
    }

    @Test
    fun `given order update has wrong-shaped primitive fields, when preview is built, then fields are omitted`() {
        val call = toolCall(
            name = "orders_update",
            arguments = buildJsonObject {
                put("id", 42)
                put("status", buildJsonObject { put("value", "processing") })
                put("billing_email", JsonArray(listOf(JsonPrimitive("buyer@example.com"))))
            },
        )

        val preview = builder.build(call)

        assertThat(preview.message).isEqualTo(
            string(
                R.string.ai_assistant_confirmation_order_update_summary,
                raw("42"),
                string(R.string.ai_assistant_confirmation_change_summary_empty),
            )
        )
        assertThat(preview.fields).isEmpty()
    }

    @Test
    fun `given order update has wrong-shaped id, when preview is built, then fallback is used`() {
        val call = toolCall(
            name = "orders_update",
            arguments = buildJsonObject {
                put("id", buildJsonObject { put("value", 42) })
                put("status", "processing")
            },
        )

        val preview = builder.build(call)

        assertThat(preview.message).isEqualTo(string(R.string.ai_assistant_confirmation_order_update_generic))
        assertThat(preview.fields).isEmpty()
    }

    @Test
    fun `given product update has wrong-shaped primitive fields, when preview is built, then fields are omitted`() {
        val call = toolCall(
            name = "products_update",
            arguments = buildJsonObject {
                put("id", 7)
                put("regular_price", buildJsonObject { put("value", "24.99") })
                put("stock_quantity", JsonArray(listOf(JsonPrimitive(100))))
                put("status", "draft")
            },
        )

        val preview = builder.build(call)

        assertThat(preview.message).isEqualTo(
            string(
                R.string.ai_assistant_confirmation_product_update_summary,
                raw("7"),
                string(R.string.ai_assistant_confirmation_change_summary_status, raw("draft")),
            )
        )
        assertThat(preview.fields).containsExactly(
            ConfirmationPreviewField(
                name = "status",
                value = raw("draft"),
                label = label(R.string.ai_assistant_confirmation_field_status),
            ),
        )
    }

    @Test
    fun `given bulk update has wrong-shaped ids, when preview is built, then fallback is used`() {
        val orderPreview = builder.build(
            toolCall(
                name = "orders_bulk_update",
                arguments = buildJsonObject {
                    put("ids", buildJsonObject { put("value", 42) })
                    put("patch", buildJsonObject { put("status", "completed") })
                },
            )
        )
        val productPreview = builder.build(
            toolCall(
                name = "products_bulk_update",
                arguments = buildJsonObject {
                    put("ids", buildJsonObject { put("value", 7) })
                    put("patch", buildJsonObject { put("regular_price", "19.99") })
                },
            )
        )

        assertThat(orderPreview.message).isEqualTo(
            string(R.string.ai_assistant_confirmation_orders_bulk_update_generic)
        )
        assertThat(orderPreview.fields).isEmpty()
        assertThat(productPreview.message).isEqualTo(
            string(R.string.ai_assistant_confirmation_products_bulk_update_generic)
        )
        assertThat(productPreview.fields).isEmpty()
    }

    @Test
    fun `given bulk update has non-numeric ids, when preview is built, then fallback is used`() {
        val orderPreview = builder.build(
            toolCall(
                name = "orders_bulk_update",
                arguments = buildJsonObject {
                    put("ids", JsonArray(listOf(JsonPrimitive(1), JsonPrimitive("bad"))))
                    put("patch", buildJsonObject { put("status", "completed") })
                },
            )
        )
        val productPreview = builder.build(
            toolCall(
                name = "products_bulk_update",
                arguments = buildJsonObject {
                    put("ids", JsonArray(listOf(JsonPrimitive(7), JsonPrimitive("bad"))))
                    put("patch", buildJsonObject { put("regular_price", "19.99") })
                },
            )
        )

        assertThat(orderPreview.message).isEqualTo(
            string(R.string.ai_assistant_confirmation_orders_bulk_update_generic)
        )
        assertThat(orderPreview.fields).isEmpty()
        assertThat(productPreview.message).isEqualTo(
            string(R.string.ai_assistant_confirmation_products_bulk_update_generic)
        )
        assertThat(productPreview.fields).isEmpty()
    }

    @Test
    fun `given bulk update has wrong-shaped patch fields, when preview is built, then fields are omitted`() {
        val orderPreview = builder.build(
            toolCall(
                name = "orders_bulk_update",
                arguments = buildJsonObject {
                    put("ids", JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2))))
                    put(
                        "patch",
                        buildJsonObject {
                            put("status", JsonArray(listOf(JsonPrimitive("completed"))))
                            put("billing_email", buildJsonObject { put("value", "buyer@example.com") })
                        },
                    )
                },
            )
        )
        val productPreview = builder.build(
            toolCall(
                name = "products_bulk_update",
                arguments = buildJsonObject {
                    put("ids", JsonArray(listOf(JsonPrimitive(7), JsonPrimitive(8))))
                    put(
                        "patch",
                        buildJsonObject {
                            put("regular_price", JsonArray(listOf(JsonPrimitive("19.99"))))
                            put("stock_quantity", buildJsonObject { put("value", 100) })
                        },
                    )
                },
            )
        )

        assertThat(orderPreview.message).isEqualTo(
            quantity(
                quantity = 2,
                singular = R.string.ai_assistant_confirmation_orders_bulk_update_summary_single,
                multiple = R.string.ai_assistant_confirmation_orders_bulk_update_summary_multiple,
                string(R.string.ai_assistant_confirmation_change_summary_empty),
            )
        )
        assertThat(orderPreview.fields).isEmpty()
        assertThat(productPreview.message).isEqualTo(
            quantity(
                quantity = 2,
                singular = R.string.ai_assistant_confirmation_products_bulk_update_summary_single,
                multiple = R.string.ai_assistant_confirmation_products_bulk_update_summary_multiple,
                string(R.string.ai_assistant_confirmation_change_summary_empty),
            )
        )
        assertThat(productPreview.fields).isEmpty()
    }

    private fun toolCall(
        name: String,
        arguments: JsonObject,
    ) = ToolCall(
        id = "call_1",
        name = name,
        arguments = arguments,
    )

    private fun label(id: Int) = string(id)

    private fun raw(value: String) = ConfirmationPreviewText.Raw(value)

    private fun string(
        id: Int,
        vararg args: ConfirmationPreviewText,
    ) = ConfirmationPreviewText.Resource(id, args.toList())

    private fun quantity(
        quantity: Int,
        singular: Int,
        multiple: Int,
        vararg args: ConfirmationPreviewText,
    ) = ConfirmationPreviewText.Quantity(quantity, singular, multiple, args.toList())

    private fun List<ConfirmationPreviewText>.toLocalizedList(): ConfirmationPreviewText =
        reduce { left, right ->
            string(R.string.ai_assistant_confirmation_message_list_separator, left, right)
        }
}
