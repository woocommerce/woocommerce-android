package com.woocommerce.android.aiassistant.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.aiassistant.R

@Composable
internal fun AssistantEmptyState(
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val suggestions = assistantEmptyStateSuggestions()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.assistant_chat_empty_state_title),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.assistant_chat_empty_state_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Column(
            modifier = Modifier.widthIn(max = EMPTY_STATE_SUGGESTIONS_MAX_WIDTH),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            suggestions.forEach { suggestion ->
                AssistantEmptyStateSuggestion(
                    text = stringResource(suggestion.promptRes),
                    onClick = onSuggestionClick,
                )
            }
        }
    }
}

@Composable
private fun AssistantEmptyStateSuggestion(
    text: String,
    onClick: (String) -> Unit,
) {
    val contentDescription = stringResource(
        R.string.assistant_chat_empty_state_suggestion_content_description,
        text,
    )

    Surface(
        onClick = { onClick(text) },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(SUGGESTION_CONTENT_PADDING),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

private data class AssistantEmptyStateSuggestionModel(
    val promptRes: Int,
)

private fun assistantEmptyStateSuggestions() = listOf(
    AssistantEmptyStateSuggestionModel(R.string.assistant_chat_empty_state_suggestion_revenue),
    AssistantEmptyStateSuggestionModel(R.string.assistant_chat_empty_state_suggestion_stock),
    AssistantEmptyStateSuggestionModel(R.string.assistant_chat_empty_state_suggestion_orders),
    AssistantEmptyStateSuggestionModel(R.string.assistant_chat_empty_state_suggestion_customers),
)

private val EMPTY_STATE_SUGGESTIONS_MAX_WIDTH = 420.dp
private val SUGGESTION_CONTENT_PADDING = PaddingValues(horizontal = 14.dp, vertical = 12.dp)

@Preview(showBackground = true, widthDp = 390, heightDp = 620)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 620, uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Large Font", showBackground = true, widthDp = 390, heightDp = 620, fontScale = 1.5f)
@Composable
private fun AssistantEmptyStatePreview() {
    Surface(color = MaterialTheme.colorScheme.background) {
        AssistantEmptyState(onSuggestionClick = {})
    }
}
