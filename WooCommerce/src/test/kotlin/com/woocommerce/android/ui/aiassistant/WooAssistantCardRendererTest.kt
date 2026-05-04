package com.woocommerce.android.ui.aiassistant

import com.woocommerce.android.R
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.util.CurrencyFormatter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class WooAssistantCardRendererTest {
    private val currencyFormatter: CurrencyFormatter = mock()

    @Test
    fun `given assistant order card, when mapped, then host row model formats total with order currency`() {
        whenever(currencyFormatter.formatCurrency("12.34", "USD")).thenReturn("$12.34")

        val model = orderCard().toOrderSummaryRowModel(currencyFormatter)

        assertThat(model.number).isEqualTo("#1001")
        assertThat(model.date).isEqualTo("2026-05-01T10:00:00Z")
        assertThat(model.customerName).isEqualTo("Jane Doe")
        assertThat(model.status).isEqualTo("processing")
        assertThat(model.statusColor).isEqualTo(R.color.tag_bg_processing)
        assertThat(model.totalPrice).isEqualTo("$12.34")
        assertThat(model.isPosOrder).isFalse()
        verify(currencyFormatter).formatCurrency("12.34", "USD")
    }

    @Test
    fun `given assistant order card without currency, when mapped, then raw total is used`() {
        val model = orderCard(currency = "").toOrderSummaryRowModel(currencyFormatter)

        assertThat(model.totalPrice).isEqualTo("12.34")
    }

    @Test
    fun `given assistant order card, when action helper is used, then open order id is used`() {
        val action = orderCard().toOpenOrderAction()

        assertThat(action).isEqualTo(AssistantCardAction.OpenOrder(remoteOrderId = 123L))
    }

    @Test
    fun `given ciab open status, when mapped, then processing color is used like dashboard orders`() {
        val model = orderCard(status = "open", currency = "").toOrderSummaryRowModel(currencyFormatter)

        assertThat(model.statusColor).isEqualTo(R.color.tag_bg_processing)
    }

    private fun orderCard(
        status: String = "processing",
        currency: String = "USD",
    ) = AssistantCard.Order(
        remoteOrderId = 123L,
        number = "#1001",
        status = status,
        total = "12.34",
        currency = currency,
        customerName = "Jane Doe",
        date = "2026-05-01T10:00:00Z",
    )
}
