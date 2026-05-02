package com.woocommerce.android.ui.aiassistant

import com.woocommerce.android.R
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.ui.orders.compose.OrderSummaryRowModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooAssistantCardRendererTest {
    @Test
    fun `given assistant order card, when mapped, then host row model uses the same displayed fields`() {
        val model = orderCard().toOrderSummaryRowModel()

        assertThat(model.number).isEqualTo("#1001")
        assertThat(model.date).isEqualTo("2026-05-01T10:00:00Z")
        assertThat(model.customerName).isEqualTo("Jane Doe")
        assertThat(model.status).isEqualTo("processing")
        assertThat(model.statusColor).isEqualTo(R.color.tag_bg_processing)
        assertThat(model.totalPrice).isEqualTo("12.34 USD")
        assertThat(model.isPosOrder).isFalse()
        assertThat(model.layoutMode).isEqualTo(OrderSummaryRowModel.LayoutMode.COMPACT)
    }

    @Test
    fun `given assistant order card, when action helper is used, then open order id is used`() {
        val action = orderCard().toOpenOrderAction()

        assertThat(action).isEqualTo(AssistantCardAction.OpenOrder(remoteOrderId = 123L))
    }

    @Test
    fun `given ciab open status, when mapped, then processing color is used like dashboard orders`() {
        val model = orderCard(status = "open").toOrderSummaryRowModel()

        assertThat(model.statusColor).isEqualTo(R.color.tag_bg_processing)
    }

    private fun orderCard(status: String = "processing") = AssistantCard.Order(
        remoteOrderId = 123L,
        number = "#1001",
        status = status,
        total = "12.34 USD",
        customerName = "Jane Doe",
        date = "2026-05-01T10:00:00Z",
    )
}
