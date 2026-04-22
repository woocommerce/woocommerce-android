package com.woocommerce.android.ui.ai.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.ai.AIAssistantViewModel.UiChatMessage
import com.woocommerce.android.ui.ai.model.MessageContent

@Composable
fun ChatMessageItem(
    message: UiChatMessage,
    onOrderClicked: (Long) -> Unit,
    onProductClicked: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        message.isStatus -> StatusMessageBubble(text = message.text, modifier = modifier)
        message.isAssistant -> AssistantMessageBubble(
            contentSegments = message.contentSegments,
            onOrderClicked = onOrderClicked,
            onProductClicked = onProductClicked,
            modifier = modifier
        )
        else -> UserMessageBubble(text = message.text, modifier = modifier)
    }
}

@Composable
private fun UserMessageBubble(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun AssistantMessageBubble(
    contentSegments: List<MessageContent>,
    onOrderClicked: (Long) -> Unit,
    onProductClicked: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.95f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            contentSegments.forEach { segment ->
                when (segment) {
                    is MessageContent.Text -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = segment.value,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is MessageContent.OrderCards -> {
                        OrderCardList(
                            orders = segment.orders,
                            onOrderClicked = onOrderClicked
                        )
                    }
                    is MessageContent.ProductCards -> {
                        ProductCardList(
                            products = segment.products,
                            onProductClicked = onProductClicked
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMessageBubble(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
