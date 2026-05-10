package com.woocommerce.android.ui.aiassistant

import android.content.Context
import com.woocommerce.android.R
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class AiAssistantVariationCardRendererTest {
    private val currencyFormatter: AiAssistantCurrencyFormatter = mock()

    @Test
    fun `given variation card, when row model is built, then product summary row fields are formatted`() {
        val context: Context = mock()
        val decimalFormatter: (BigDecimal) -> String = { amount -> "\$${amount.toPlainString()}" }
        whenever(context.getString(R.string.product_status_draft)).thenReturn("Draft")
        whenever(context.getString(R.string.product_stock_status_instock)).thenReturn("In stock")
        whenever(context.getString(R.string.orderdetail_product_lineitem_sku_value, "woo-socks-blue"))
            .thenReturn("SKU: woo-socks-blue")
        whenever(currencyFormatter.buildBigDecimalFormatter()).thenReturn(decimalFormatter)

        val model = variationCard().toVariationSummaryRowModel(context, currencyFormatter)

        assertThat(model).isEqualTo(
            AssistantVariationSummaryRowModel(
                title = "Blue socks",
                imageUrl = "https://example.com/blue-socks.png",
                supportingTexts = listOf(
                    "Size: M \u2022 Color: Blue",
                    "Draft \u2022 In stock \u2022 \$12.99",
                    "SKU: woo-socks-blue",
                ),
            )
        )
    }

    @Test
    fun `given variation card has blank supporting fields, when row model is built, then blank rows are omitted`() {
        val context: Context = mock()

        val model = variationCard(
            sku = "",
            price = "",
            stockStatus = "",
            status = "publish",
            attributes = emptyList(),
        ).toVariationSummaryRowModel(context, currencyFormatter)

        assertThat(model.supportingTexts).isEmpty()
    }

    @Test
    fun `given variation card has non numeric price, when row model is built, then raw price is used`() {
        val context: Context = mock()

        val model = variationCard(
            sku = "",
            price = "From 12.99",
            stockStatus = "",
            status = "publish",
            attributes = emptyList(),
        ).toVariationSummaryRowModel(context, currencyFormatter)

        assertThat(model.supportingTexts).containsExactly("From 12.99")
    }

    @Test
    fun `given variation card has blank name and non blank sku, when row model is built, then title uses sku label`() {
        val context: Context = mock()
        whenever(context.getString(R.string.orderdetail_product_lineitem_sku_value, "woo-socks-blue"))
            .thenReturn("SKU: woo-socks-blue")

        val model = variationCard(name = "", price = "", stockStatus = "", status = "publish")
            .toVariationSummaryRowModel(context, currencyFormatter)

        assertThat(model.title).isEqualTo("SKU: woo-socks-blue")
    }

    @Test
    fun `given variation card has blank name and no sku, when row model is built, then title uses variation id label`() {
        val context: Context = mock()
        whenever(context.getString(R.string.ai_assistant_variation_card_id_title, 10L)).thenReturn("Variation 10")

        val model = variationCard(name = "", sku = "", price = "", stockStatus = "", status = "publish")
            .toVariationSummaryRowModel(context, currencyFormatter)

        assertThat(model.title).isEqualTo("Variation 10")
    }

    @Test
    fun `given variation card, when open action is built, then both ids are emitted`() {
        val action = variationCard().toOpenProductVariationAction()

        assertThat(action).isEqualTo(
            com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction.OpenProductVariation(
                parentProductId = 100L,
                variationId = 10L,
            )
        )
    }

    private fun variationCard(
        name: String = "Blue socks",
        sku: String = "woo-socks-blue",
        price: String = "12.99",
        stockStatus: String = "instock",
        status: String = "draft",
        attributes: List<AssistantCard.Variation.Attribute> = listOf(
            AssistantCard.Variation.Attribute(name = "Size", option = "M"),
            AssistantCard.Variation.Attribute(name = "Color", option = "Blue"),
        ),
    ) = AssistantCard.Variation(
        parentProductId = 100L,
        variationId = 10L,
        name = name,
        sku = sku,
        price = price,
        stockStatus = stockStatus,
        status = status,
        imageUrl = "https://example.com/blue-socks.png",
        attributes = attributes,
    )
}
