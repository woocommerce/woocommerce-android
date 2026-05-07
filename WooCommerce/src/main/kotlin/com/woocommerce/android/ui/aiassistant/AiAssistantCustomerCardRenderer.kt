package com.woocommerce.android.ui.aiassistant

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction

internal class AiAssistantCustomerCardRenderer {
    @Composable
    fun Card(
        card: AssistantCard.Customer,
        onAction: (AssistantCardAction) -> Unit,
        modifier: Modifier,
    ) {
        val context = LocalContext.current
        val rowModel = card.toCustomerSummaryRowModel(context)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .clickable(
                    enabled = true,
                    role = Role.Button,
                    onClick = { onAction(AssistantCardAction.OpenCustomer(card.remoteCustomerId)) },
                )
                .padding(
                    horizontal = dimensionResource(id = R.dimen.major_100),
                    vertical = dimensionResource(id = R.dimen.minor_100),
                )
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(
                        text = rowModel.title,
                        color = colorResource(id = R.color.color_on_surface),
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.W500,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = rowModel.emailText,
                    color = colorResource(id = R.color.color_on_surface),
                    style = MaterialTheme.typography.body2,
                )
            }
        }
    }
}

internal data class AssistantCustomerSummaryRowModel(
    val title: String,
    val emailText: String,
)

internal fun AssistantCard.Customer.toCustomerSummaryRowModel(context: Context): AssistantCustomerSummaryRowModel =
    AssistantCustomerSummaryRowModel(
        title = name.ifBlank { context.getString(R.string.order_creation_customer_search_empty_name) },
        emailText = email.ifBlank { context.getString(R.string.order_creation_customer_search_empty_email) },
    )
