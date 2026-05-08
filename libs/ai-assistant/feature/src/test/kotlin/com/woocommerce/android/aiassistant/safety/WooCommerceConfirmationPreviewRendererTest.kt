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
    fun `given order status update that emails customer, when preview is rendered, then message uses summary`() {
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
                R.string.ai_assistant_confirmation_order_update_summary,
                "42",
            )
        )
        assertThat(rendered.message).isEqualTo(
            context.getString(
                R.string.ai_assistant_confirmation_order_update_summary,
                "42",
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
            ),
            snapshot = ConfirmationSnapshot(
                currentValues = mapOf(
                    "status" to "pending",
                )
            ),
        )

        val rendered = renderer.render(preview)

        assertThat(rendered.summary).isEqualTo("Update order #42: emails the customer")
        assertThat(rendered.rows).containsExactly(
            RenderedConfirmationDiffRow(
                name = "status",
                label = context.getString(R.string.ai_assistant_confirmation_field_status),
                value = "processing",
                beforeValue = "pending",
            )
        )
        assertThat(rendered.isBulk).isFalse()
    }

    @Test
    fun `given bulk update, when preview is rendered, then before values stay absent`() {
        val preview = builder.build(
            toolCall(
                name = "products_bulk_update",
                arguments = buildJsonObject {
                    put("ids", JsonArray(listOf(JsonPrimitive(7))))
                    put(
                        "patch",
                        buildJsonObject {
                            put("status", "draft")
                        },
                    )
                },
            )
        )

        val rendered = renderer.render(preview)

        assertThat(rendered.isBulk).isTrue()
        assertThat(rendered.rows.single().beforeValue).isNull()
        assertThat(rendered.rows.single().afterValue).isEqualTo("draft")
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

        assertThat(rendered.message).isEqualTo("Update 1 order: emails the customer")
        assertThat(rendered.rows).containsExactly(
            RenderedConfirmationDiffRow(
                name = "status",
                label = context.getString(R.string.ai_assistant_confirmation_field_status),
                value = "completed",
            )
        )
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

        assertThat(rendered.message).isEqualTo("Update 2 products")
    }

    @Test
    fun `given product snapshot has name, when preview is rendered, then message includes product name`() {
        val preview = builder.build(
            toolCall(
                name = "products_update",
                arguments = buildJsonObject {
                    put("id", 7)
                    put("regular_price", "24.99")
                },
            ),
            snapshot = ConfirmationSnapshot(
                currentValues = mapOf(
                    "name" to "Classic T-Shirt",
                    "regular_price" to "19.99",
                )
            ),
        )

        val rendered = renderer.render(preview)

        assertThat(rendered.message).isEqualTo("Update product Classic T-Shirt (#7)")
        assertThat(rendered.rows).containsExactly(
            RenderedConfirmationDiffRow(
                name = "regular_price",
                label = context.getString(R.string.ai_assistant_confirmation_field_regular_price),
                value = "24.99",
                beforeValue = "19.99",
            )
        )
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

        assertThat(rendered.message).isEqualTo("Update variation #8 for product #7")
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
