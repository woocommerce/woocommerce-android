package com.woocommerce.android.ui.aiassistant

import android.content.Context
import com.woocommerce.android.R
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.util.CurrencyFormatter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class AiAssistantProductCardRendererTest {
    private val currencyFormatter: CurrencyFormatter = mock()

    @Test
    fun `given assistant product card, when row model is built, then product image url is preserved`() {
        val context: Context = mock()
        val decimalFormatter: (BigDecimal) -> String = { amount -> "\$${amount.toPlainString()}" }
        whenever(context.getString(R.string.product_stock_status_instock)).thenReturn("In stock")
        whenever(context.getString(R.string.orderdetail_product_lineitem_sku_value, "woo-socks"))
            .thenReturn("SKU: woo-socks")
        whenever(currencyFormatter.buildBigDecimalFormatter()).thenReturn(decimalFormatter)

        val model = productCard().toProductSummaryRowModel(context, currencyFormatter)

        assertThat(model).isEqualTo(
            AssistantProductSummaryRowModel(
                title = "Socks",
                imageUrl = "https://example.com/socks.png",
                stockStatusPriceText = "In stock \u2022 \$9.99",
                skuText = "SKU: woo-socks",
            )
        )
    }

    @Test
    fun `given assistant product card with non-numeric price, when row model is built, then raw price is used`() {
        val context: Context = mock()
        whenever(context.getString(R.string.product_stock_status_instock)).thenReturn("In stock")

        val model = productCard(price = "Free").toProductSummaryRowModel(context, currencyFormatter)

        assertThat(model.stockStatusPriceText).isEqualTo("In stock \u2022 Free")
    }

    private fun productCard(
        price: String = "9.99",
    ) = AssistantCard.Product(
        remoteProductId = 456L,
        name = "Socks",
        sku = "woo-socks",
        price = price,
        stockStatus = "instock",
        status = "publish",
        imageUrl = "https://example.com/socks.png",
    )
}
