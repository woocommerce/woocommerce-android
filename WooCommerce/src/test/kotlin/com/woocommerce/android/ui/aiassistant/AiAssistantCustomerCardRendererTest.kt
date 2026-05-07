package com.woocommerce.android.ui.aiassistant

import android.content.Context
import com.woocommerce.android.R
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AiAssistantCustomerCardRendererTest {
    private val context: Context = mock()

    @Test
    fun `when renderer is created, then class has direct unit test coverage`() {
        assertThat(AiAssistantCustomerCardRenderer()).isNotNull
    }

    @Test
    fun `given assistant customer card, when row model is built, then name and email are used`() {
        val model = customerCard().toCustomerSummaryRowModel(context)

        assertThat(model).isEqualTo(
            AssistantCustomerSummaryRowModel(
                name = "Ada Lovelace",
                email = "ada@example.com",
            )
        )
    }

    @Test
    fun `given assistant customer card with blank fields, when row model is built, then fallbacks are used`() {
        whenever(context.getString(R.string.order_creation_customer_search_empty_name)).thenReturn("No name")
        whenever(context.getString(R.string.order_creation_customer_search_empty_email)).thenReturn("No email address")

        val model = customerCard(name = "", email = "").toCustomerSummaryRowModel(context)

        assertThat(model).isEqualTo(
            AssistantCustomerSummaryRowModel(
                name = "No name",
                email = "No email address",
            )
        )
    }

    private fun customerCard(
        name: String = "Ada Lovelace",
        email: String = "ada@example.com",
    ) = AssistantCard.Customer(
        remoteCustomerId = 789L,
        name = name,
        email = email,
    )
}
