package com.woocommerce.android.aiassistant.ui.cards

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantCardActionTest {
    @Test
    fun `given open order action, when created, then remote order id is preserved`() {
        val action: AssistantCardAction = AssistantCardAction.OpenOrder(remoteOrderId = 123L)

        assertThat(action).isEqualTo(AssistantCardAction.OpenOrder(remoteOrderId = 123L))
    }

    @Test
    fun `given open product action, when created, then remote product id is preserved`() {
        val action: AssistantCardAction = AssistantCardAction.OpenProduct(remoteProductId = 456L)

        assertThat(action).isEqualTo(AssistantCardAction.OpenProduct(remoteProductId = 456L))
    }

    @Test
    fun `given open product variation action, when created, then parent product and variation ids are preserved`() {
        val action: AssistantCardAction = AssistantCardAction.OpenProductVariation(
            parentProductId = 100L,
            variationId = 10L,
        )

        assertThat(action).isEqualTo(
            AssistantCardAction.OpenProductVariation(
                parentProductId = 100L,
                variationId = 10L,
            )
        )
    }

    @Test
    fun `given open customer action, when created, then remote customer id is preserved`() {
        val action: AssistantCardAction = AssistantCardAction.OpenCustomer(remoteCustomerId = 789L)

        assertThat(action).isEqualTo(AssistantCardAction.OpenCustomer(remoteCustomerId = 789L))
    }

    @Test
    fun `given open analytics action, when created, then date range strings are preserved`() {
        val action: AssistantCardAction = AssistantCardAction.OpenAnalytics(
            after = "2026-05-01",
            before = "2026-05-07",
        )

        assertThat(action).isEqualTo(
            AssistantCardAction.OpenAnalytics(
                after = "2026-05-01",
                before = "2026-05-07",
            )
        )
    }
}
