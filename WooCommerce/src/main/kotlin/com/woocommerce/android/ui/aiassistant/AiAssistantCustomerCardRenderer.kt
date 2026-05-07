package com.woocommerce.android.ui.aiassistant

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
        CustomerSummaryRow(
            name = card.name.ifBlank { stringResource(R.string.order_creation_customer_search_empty_name) },
            email = card.email.ifBlank { stringResource(R.string.order_creation_customer_search_empty_email) },
            onClick = { onAction(AssistantCardAction.OpenCustomer(card.remoteCustomerId)) },
            modifier = modifier,
        )
    }
}
