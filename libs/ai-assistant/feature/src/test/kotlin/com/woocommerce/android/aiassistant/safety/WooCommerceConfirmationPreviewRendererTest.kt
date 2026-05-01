package com.woocommerce.android.aiassistant.safety

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WooCommerceConfirmationPreviewRendererTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val builder = WooCommerceConfirmationPreviewBuilder()
    private val renderer = ConfirmationPreviewRenderer(context)

    @Test
    fun `given order status update, when preview is rendered, then message uses string resource`() {
        val preview = builder.build(
            toolCall(
                name = "orders_update",
                arguments = buildJsonObject {
                    put("id", 42)
                    put("status", "processing")
                },
            )
        )

        val rendered = renderer.render(preview)

        assertThat(rendered.summary).isEqualTo(
            context.getString(
                R.string.ai_assistant_confirmation_order_set_status_emails_customer,
                "42",
                "processing",
            )
        )
        assertThat(rendered.message).isEqualTo(
            context.getString(
                R.string.ai_assistant_confirmation_order_set_status_emails_customer,
                "42",
                "processing",
            )
        )
        assertThat(rendered.fields).containsExactly(
            RenderedConfirmationPreviewField(
                name = "status",
                label = context.getString(R.string.ai_assistant_confirmation_field_status),
                value = "processing",
            )
        )
    }

    @Test
    fun `given order status update, when preview is rendered, then summary and rows are exposed for inline cards`() {
        val preview = builder.build(
            toolCall(
                name = "orders_update",
                arguments = buildJsonObject {
                    put("id", 42)
                    put("status", "processing")
                },
            )
        )

        val rendered = renderer.render(preview)

        assertThat(rendered.summary).isEqualTo("Set order #42 to processing (emails the customer)")
        assertThat(rendered.rows).containsExactly(
            RenderedConfirmationDiffRow(
                name = "status",
                label = context.getString(R.string.ai_assistant_confirmation_field_status),
                value = "processing",
            )
        )
        assertThat(rendered.isBulk).isFalse()
    }

    @Test
    fun `given one order in bulk update, when preview is rendered, then singular string is used`() {
        val preview = builder.build(
            toolCall(
                name = "orders_bulk_update",
                arguments = buildJsonObject {
                    put("ids", JsonArray(listOf(JsonPrimitive(42))))
                    put("patch", buildJsonObject { put("status", "completed") })
                },
            )
        )

        val rendered = renderer.render(preview)

        assertThat(rendered.message).isEqualTo("Update 1 order: status -> completed (emails the customer)")
    }

    @Test
    fun `given many products in bulk update, when preview is rendered, then multiple string is used`() {
        val preview = builder.build(
            toolCall(
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
        )

        val rendered = renderer.render(preview)

        assertThat(rendered.message).isEqualTo("Update 2 products: price -> 19.99, status -> draft")
    }

    @Test
    fun `given variation update, when preview is rendered, then nested messages are localized`() {
        val preview = builder.build(
            toolCall(
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
        )

        val rendered = renderer.render(preview)

        assertThat(rendered.message).isEqualTo(
            "Update variation #8 for product #7: " +
                "price -> 19.99, sale -> Off, stock -> 3, stock status -> instock, status -> publish, SKU -> VAR-8"
        )
        assertThat(rendered.fields).contains(
            RenderedConfirmationPreviewField(
                name = "sale_price",
                label = context.getString(R.string.ai_assistant_confirmation_field_sale_price),
                value = context.getString(R.string.ai_assistant_confirmation_field_value_off),
            )
        )
    }

    @Test
    fun `given unknown tool, when preview is rendered, then generic resource omits raw arguments`() {
        val preview = builder.build(
            toolCall(
                name = "custom_tool",
                arguments = buildJsonObject { put("id", 1) },
            )
        )

        val rendered = renderer.render(preview)

        assertThat(rendered.message).isEqualTo("Review custom_tool")
        assertThat(rendered.message).doesNotContain("id")
        assertThat(rendered.fields).isEmpty()
    }

    private fun toolCall(
        name: String,
        arguments: JsonObject,
    ) = ToolCall(
        id = "call_1",
        name = name,
        arguments = arguments,
    )
}
