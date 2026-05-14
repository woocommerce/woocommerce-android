package com.woocommerce.android.aiassistant.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.ui.assistantCanvasColor
import com.woocommerce.android.aiassistant.ui.assistantOutlineColor

@Composable
internal fun AssistantEmptyState(
    showEarlyAccessNotice: Boolean,
    bottomContentPadding: Dp,
    onFeedbackClick: () -> Unit,
    onDismissEarlyAccessNotice: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val suggestions = assistantEmptyStateSuggestions()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (showEarlyAccessNotice) {
            AssistantEarlyAccessNoticeCard(
                onFeedbackClick = onFeedbackClick,
                onDismissClick = onDismissEarlyAccessNotice,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = stringResource(R.string.ai_assistant_chat_empty_state_title),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(EMPTY_STATE_CARD_CORNER_RADIUS),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, assistantOutlineColor()),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                suggestions.forEachIndexed { index, suggestion ->
                    AssistantEmptyStateSuggestionRow(
                        iconRes = suggestion.iconRes,
                        promptRes = suggestion.promptRes,
                        onClick = onSuggestionClick,
                    )
                    if (index < suggestions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = EMPTY_STATE_DIVIDER_INDENT),
                            color = assistantOutlineColor().copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(bottomContentPadding))
    }
}

@Composable
private fun AssistantEmptyStateSuggestionRow(
    @DrawableRes iconRes: Int,
    @StringRes promptRes: Int,
    onClick: (String) -> Unit,
) {
    val prompt = stringResource(promptRes)
    val rowContentDescription = stringResource(
        R.string.ai_assistant_chat_empty_state_suggestion_content_description,
        prompt,
    )

    Surface(
        onClick = { onClick(prompt) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = EMPTY_STATE_ROW_MIN_HEIGHT)
            .semantics { contentDescription = rowContentDescription },
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EMPTY_STATE_ROW_HORIZONTAL_PADDING, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(EMPTY_STATE_ROW_ICON_SPACING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(EMPTY_STATE_ICON_SIZE),
            )
            Text(
                text = prompt,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private data class AssistantEmptyStateSuggestionModel(
    @DrawableRes val iconRes: Int,
    @StringRes val promptRes: Int,
)

private fun assistantEmptyStateSuggestions() = listOf(
    AssistantEmptyStateSuggestionModel(
        iconRes = R.drawable.ic_assistant_empty_state_revenue,
        promptRes = R.string.ai_assistant_chat_empty_state_suggestion_revenue,
    ),
    AssistantEmptyStateSuggestionModel(
        iconRes = R.drawable.ic_assistant_empty_state_inventory,
        promptRes = R.string.ai_assistant_chat_empty_state_suggestion_stock,
    ),
    AssistantEmptyStateSuggestionModel(
        iconRes = R.drawable.ic_assistant_empty_state_orders,
        promptRes = R.string.ai_assistant_chat_empty_state_suggestion_orders,
    ),
    AssistantEmptyStateSuggestionModel(
        iconRes = R.drawable.ic_assistant_empty_state_customers,
        promptRes = R.string.ai_assistant_chat_empty_state_suggestion_customers,
    ),
)

private val EMPTY_STATE_CARD_CORNER_RADIUS = 12.dp
private val EMPTY_STATE_ROW_MIN_HEIGHT = 56.dp
private val EMPTY_STATE_ROW_HORIZONTAL_PADDING = 16.dp
private val EMPTY_STATE_ROW_ICON_SPACING = 16.dp
private val EMPTY_STATE_ICON_SIZE = 24.dp
private val EMPTY_STATE_DIVIDER_INDENT = 56.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 620)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 620, uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Large Font", showBackground = true, widthDp = 390, heightDp = 620, fontScale = 1.5f)
@Composable
private fun AssistantEmptyStatePreview() {
    Surface(color = assistantCanvasColor()) {
        AssistantEmptyState(
            showEarlyAccessNotice = true,
            bottomContentPadding = 16.dp,
            onFeedbackClick = {},
            onDismissEarlyAccessNotice = {},
            onSuggestionClick = {},
        )
    }
}
