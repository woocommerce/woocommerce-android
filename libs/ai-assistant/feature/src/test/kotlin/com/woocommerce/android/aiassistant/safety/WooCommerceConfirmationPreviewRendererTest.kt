package com.woocommerce.android.aiassistant.safety

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.woocommerce.android.aiassistant.R
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WooCommerceConfirmationPreviewRendererTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val renderer = ConfirmationPreviewRenderer(context)

    @Test
    fun `given order status update that emails customer, when preview is rendered, then message uses summary`() {
        val preview = ConfirmationPreview(
            message = string(
                R.string.ai_assistant_confirmation_order_update_summary,
                raw("42"),
            ),
            fields = listOf(
                field(
                    name = "status",
                    label = R.string.ai_assistant_confirmation_field_status,
                    value = raw("processing"),
                )
            ),
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
        val preview = ConfirmationPreview(
            message = string(
                R.string.ai_assistant_confirmation_order_update_summary,
                raw("42"),
            ),
            fields = listOf(
                field(
                    name = "status",
                    label = R.string.ai_assistant_confirmation_field_status,
                    value = raw("processing"),
                    beforeValue = raw("pending"),
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
        val preview = ConfirmationPreview(
            message = quantity(
                quantity = 1,
                singular = R.string.ai_assistant_confirmation_products_bulk_update_title_single,
                multiple = R.string.ai_assistant_confirmation_products_bulk_update_title_multiple,
            ),
            fields = listOf(
                field(
                    name = "status",
                    label = R.string.ai_assistant_confirmation_field_status,
                    value = raw("draft"),
                )
            ),
            isBulk = true,
            bulkEntries = listOf(
                ConfirmationBulkEntry(7, "Classic T-Shirt"),
                ConfirmationBulkEntry(8),
            ),
        )

        val rendered = renderer.render(preview)

        assertThat(rendered.isBulk).isTrue()
        assertThat(rendered.rows.single().beforeValue).isNull()
        assertThat(rendered.rows.single().afterValue).isEqualTo("draft")
        assertThat(rendered.bulkEntries).containsExactly(
            ConfirmationBulkEntry(7, "Classic T-Shirt"),
            ConfirmationBulkEntry(8),
        )
        assertThat(rendered.bulkEntries.first().displayText).isEqualTo("#7  Classic T-Shirt")
    }

    @Test
    fun `given one order in bulk update, when preview is rendered, then singular string is used`() {
        val preview = ConfirmationPreview(
            message = quantity(
                quantity = 1,
                singular = R.string.ai_assistant_confirmation_orders_bulk_update_summary_single,
                multiple = R.string.ai_assistant_confirmation_orders_bulk_update_summary_multiple,
            ),
            fields = listOf(
                field(
                    name = "status",
                    label = R.string.ai_assistant_confirmation_field_status,
                    value = raw("completed"),
                )
            ),
            isBulk = true,
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
        val preview = ConfirmationPreview(
            message = quantity(
                quantity = 2,
                singular = R.string.ai_assistant_confirmation_products_bulk_update_title_single,
                multiple = R.string.ai_assistant_confirmation_products_bulk_update_title_multiple,
            )
        )

        val rendered = renderer.render(preview)

        assertThat(rendered.message).isEqualTo("Update 2 products")
    }

    @Test
    fun `given product snapshot has name, when preview is rendered, then message includes product name`() {
        val preview = ConfirmationPreview(
            message = string(
                R.string.ai_assistant_confirmation_product_update_title_with_name,
                raw("Classic T-Shirt"),
                raw("7"),
            ),
            fields = listOf(
                field(
                    name = "regular_price",
                    label = R.string.ai_assistant_confirmation_field_regular_price,
                    value = raw("24.99"),
                    beforeValue = raw("19.99"),
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
        val preview = ConfirmationPreview(
            message = string(
                R.string.ai_assistant_confirmation_product_variation_update_title,
                raw("8"),
                raw("7"),
            ),
            fields = listOf(
                field(
                    name = "sale_price",
                    label = R.string.ai_assistant_confirmation_field_sale_price,
                    value = string(R.string.ai_assistant_confirmation_field_value_off),
                )
            ),
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
        val preview = ConfirmationPreview(
            message = string(R.string.ai_assistant_confirmation_generic_tool_call, raw("custom_tool"))
        )

        val rendered = renderer.render(preview)

        assertThat(rendered.message).isEqualTo("Review custom_tool")
        assertThat(rendered.message).doesNotContain("id")
        assertThat(rendered.fields).isEmpty()
    }

    private fun field(
        name: String,
        label: Int,
        value: ConfirmationPreviewText,
        beforeValue: ConfirmationPreviewText? = null,
    ) = ConfirmationPreviewField(
        name = name,
        label = string(label),
        value = value,
        beforeValue = beforeValue,
    )

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
}
