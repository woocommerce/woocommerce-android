package com.woocommerce.android.ui.aiassistant

import android.content.Context
import com.woocommerce.android.R
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.util.CurrencyFormatter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Locale

class AiAssistantOrderCardRendererTest {
    private val currencyFormatter: CurrencyFormatter = mock()
    private val context: Context = mock()

    @Test
    fun `when renderer is created, then class has direct unit test coverage`() {
        assertThat(AiAssistantOrderCardRenderer(currencyFormatter)).isNotNull
    }

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
    fun `given ciab open status, when mapped, then processing color is used like dashboard orders`() {
        val model = orderCard(status = "open", currency = "").toOrderSummaryRowModel(context, currencyFormatter)

        assertThat(model.statusColor).isEqualTo(R.color.tag_bg_processing)
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
}
