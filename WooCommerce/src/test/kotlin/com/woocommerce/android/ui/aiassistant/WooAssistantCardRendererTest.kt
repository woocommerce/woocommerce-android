package com.woocommerce.android.ui.aiassistant

import android.content.Context
import com.woocommerce.android.R
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AiAssistantStatsCardState
import com.woocommerce.android.util.CurrencyFormatter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Locale

class WooAssistantCardRendererTest {
    private val currencyFormatter: CurrencyFormatter = mock()
    private val context: Context = mock()

    @Test
    fun `given assistant order card, when mapped, then host row model formats total with order currency`() {
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        whenever(currencyFormatter.formatCurrency("12.34", "USD")).thenReturn("$12.34")

        val model = try {
            orderCard().toOrderSummaryRowModel(context, currencyFormatter)
        } finally {
            Locale.setDefault(originalLocale)
        }

        assertThat(model.number).isEqualTo("#1001")
        assertThat(model.date).matches("^May [12]$")
        assertThat(model.customerName).isEqualTo("Jane Doe")
        assertThat(model.status).isEqualTo("processing")
        assertThat(model.statusColor).isEqualTo(R.color.tag_bg_processing)
        assertThat(model.totalPrice).isEqualTo("$12.34")
        assertThat(model.isPosOrder).isFalse()
        verify(currencyFormatter).formatCurrency("12.34", "USD")
    }

    @Test
    fun `given assistant order card without currency, when mapped, then raw total is used`() {
        val model = orderCard(currency = "").toOrderSummaryRowModel(context, currencyFormatter)

        assertThat(model.totalPrice).isEqualTo("12.34")
    }

    @Test
    fun `given assistant order card with unparseable date, when mapped, then raw date is preserved`() {
        val model = orderCard(currency = "", date = "not-a-date").toOrderSummaryRowModel(context, currencyFormatter)

        assertThat(model.date).isEqualTo("not-a-date")
    }

    @Test
    fun `given assistant order card with blank customer name, when mapped, then guest fallback is used`() {
        whenever(context.getString(R.string.orderdetail_customer_name_default)).thenReturn("Guest")

        val model = orderCard(currency = "", customerName = "").toOrderSummaryRowModel(context, currencyFormatter)

        assertThat(model.customerName).isEqualTo("Guest")
    }

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

    @Test
    fun `given ciab open status, when mapped, then processing color is used like dashboard orders`() {
        val model = orderCard(status = "open", currency = "").toOrderSummaryRowModel(context, currencyFormatter)

        assertThat(model.statusColor).isEqualTo(R.color.tag_bg_processing)
    }

    @Test
    fun `given assistant stats card, when mapped, then period revenue order count and chart values are displayed`() {
        whenever(currencyFormatter.formatCurrency("123.45", "USD")).thenReturn("$123.45")

        val model = statsCard(revenueCurrency = "USD").toStatsCardState(currencyFormatter, Locale.US)

        assertThat(model.period).isEqualTo("May 1 - May 7, 2026")
        assertThat(model.revenueTotal).isEqualTo("$123.45")
        assertThat(model.orderCount).isEqualTo("8")
        assertThat(model.chartValues).containsExactly(12.0, 18.0, 9.0)
        assertThat(model.isTrendAvailable).isTrue()
        verify(currencyFormatter).formatCurrency("123.45", "USD")
    }

    @Test
    fun `given assistant stats card with one day range, when mapped, then single date is displayed`() {
        assertThat(statsCard(after = "2026-05-01", before = "2026-05-01")
            .toStatsCardState(currencyFormatter, Locale.US).period)
            .isEqualTo("May 1, 2026")
    }

    @Test
    fun `given assistant stats card with blank metrics, when mapped, then unavailable fallback is used`() {
        val model = statsCard(revenueTotal = "", orderCount = "", chartPoints = emptyList())
            .toStatsCardState(currencyFormatter, Locale.US)

        assertThat(model).isEqualTo(
            AiAssistantStatsCardState(
                period = "May 1 - May 7, 2026",
                revenueTotal = "Unavailable",
                orderCount = "Unavailable",
                chartValues = emptyList(),
                isTrendAvailable = false,
            )
        )
    }

    @Test
    fun `given assistant stats card with no chart points, when mapped, then trend is unavailable`() {
        val model = statsCard(chartPoints = emptyList()).toStatsCardState(currencyFormatter, Locale.US)

        assertThat(model.chartValues).isEmpty()
        assertThat(model.isTrendAvailable).isFalse()
    }

    @Test
    fun `given assistant stats card with invalid dates, when mapped, then raw date range is preserved`() {
        assertThat(statsCard(after = "bad", before = "2026-05-07")
            .toStatsCardState(currencyFormatter, Locale.US).period)
            .isEqualTo("bad - 2026-05-07")
    }

    @Test
    fun `given assistant stats card without currency, when mapped, then raw revenue is used`() {
        assertThat(statsCard(revenueCurrency = "")
            .toStatsCardState(currencyFormatter, Locale.US).revenueTotal)
            .isEqualTo("123.45")
    }

    private fun orderCard(
        status: String = "processing",
        currency: String = "USD",
        date: String = "2026-05-01T10:00:00Z",
        customerName: String = "Jane Doe",
    ) = AssistantCard.Order(
        remoteOrderId = 123L,
        number = "#1001",
        status = status,
        total = "12.34",
        currency = currency,
        customerName = customerName,
        date = date,
    )

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

    private fun statsCard(
        after: String = "2026-05-01",
        before: String = "2026-05-07",
        revenueTotal: String = "123.45",
        revenueCurrency: String = "",
        orderCount: String = "8",
        chartPoints: List<AssistantCard.Stats.ChartPoint> = listOf(
            AssistantCard.Stats.ChartPoint("2026-05-01", 12.0),
            AssistantCard.Stats.ChartPoint("2026-05-02", 18.0),
            AssistantCard.Stats.ChartPoint("2026-05-03", 9.0),
        ),
    ) = AssistantCard.Stats(
        after = after,
        before = before,
        revenueTotal = revenueTotal,
        revenueCurrency = revenueCurrency,
        orderCount = orderCount,
        chartPoints = chartPoints,
    )
}
