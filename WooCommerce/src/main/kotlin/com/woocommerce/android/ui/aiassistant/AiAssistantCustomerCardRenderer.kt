package com.woocommerce.android.ui.aiassistant

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.woocommerce.android.R
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.ui.customer.compose.CustomerSummaryRow

internal class AiAssistantCustomerCardRenderer {
    @Composable
    fun Card(
        card: AssistantCard.Customer,
        onAction: (AssistantCardAction) -> Unit,
        modifier: Modifier,
    ) {
        val context = LocalContext.current
        val rowModel = card.toCustomerSummaryRowModel(context)
        CustomerSummaryRow(
            name = rowModel.name,
            email = rowModel.email,
            onClick = { onAction(AssistantCardAction.OpenCustomer(card.remoteCustomerId)) },
            modifier = modifier,
        )
    }
}

internal data class AssistantCustomerSummaryRowModel(
    val name: String,
    val email: String,
)

internal fun AssistantCard.Customer.toCustomerSummaryRowModel(context: Context): AssistantCustomerSummaryRowModel =
    AssistantCustomerSummaryRowModel(
        name = name.ifBlank { context.getString(R.string.order_creation_customer_search_empty_name) },
        email = email.ifBlank { context.getString(R.string.order_creation_customer_search_empty_email) },
    )
