package com.woocommerce.android.aiassistant.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.ui.assistantCanvasColor
import com.woocommerce.android.aiassistant.ui.assistantOutlineColor

@Composable
internal fun AssistantEarlyAccessNoticeCard(
    onFeedbackClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(EARLY_ACCESS_CARD_CORNER_RADIUS),
        color = earlyAccessContainerColor(),
        contentColor = earlyAccessContentColor(),
        border = BorderStroke(1.dp, earlyAccessBorderColor()),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(EARLY_ACCESS_CARD_PADDING),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AssistantEarlyAccessBadge()
                Text(
                    text = stringResource(R.string.assistant_early_access_notice_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = earlyAccessContentColor(),
                )
                AssistantEarlyAccessFeedbackButton(onClick = onFeedbackClick)
            }
            IconButton(
                onClick = onDismissClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_assistant_notice_dismiss),
                    contentDescription = stringResource(
                        R.string.assistant_early_access_notice_dismiss_content_description
                    ),
                    tint = earlyAccessMutedContentColor(),
                )
            }
        }
    }
}

@Composable
private fun AssistantEarlyAccessBadge() {
    Surface(
        shape = RoundedCornerShape(EARLY_ACCESS_BADGE_CORNER_RADIUS),
        color = earlyAccessAccentColor(),
        contentColor = Color.White,
    ) {
        Text(
            text = stringResource(R.string.assistant_early_access_notice_badge),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AssistantEarlyAccessFeedbackButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(EARLY_ACCESS_BUTTON_CORNER_RADIUS),
        color = earlyAccessButtonColor(),
        contentColor = earlyAccessAccentColor(),
        border = BorderStroke(1.dp, earlyAccessBorderColor()),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_assistant_feedback),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = earlyAccessAccentColor(),
            )
            Text(
                text = stringResource(R.string.assistant_early_access_notice_feedback),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = earlyAccessAccentColor(),
            )
        }
    }
}

@Composable
private fun earlyAccessContainerColor(): Color = if (isSystemInDarkTheme()) {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
} else {
    Color(0xFFF3E9FF)
}

@Composable
private fun earlyAccessButtonColor(): Color = if (isSystemInDarkTheme()) {
    MaterialTheme.colorScheme.surface
} else {
    Color.White
}

@Composable
private fun earlyAccessContentColor(): Color = if (isSystemInDarkTheme()) {
    MaterialTheme.colorScheme.onSurface
} else {
    Color(0xFF2F213B)
}

@Composable
private fun earlyAccessMutedContentColor(): Color = earlyAccessContentColor().copy(alpha = 0.72f)

@Composable
private fun earlyAccessBorderColor(): Color = if (isSystemInDarkTheme()) {
    assistantOutlineColor()
} else {
    Color(0xFFE0C7FF)
}

@Composable
private fun earlyAccessAccentColor(): Color = Color(0xFF7F54B3)

private val EARLY_ACCESS_CARD_CORNER_RADIUS = 16.dp
private val EARLY_ACCESS_BADGE_CORNER_RADIUS = 6.dp
private val EARLY_ACCESS_BUTTON_CORNER_RADIUS = 8.dp
private val EARLY_ACCESS_CARD_PADDING = 16.dp

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun AssistantEarlyAccessNoticeCardPreview() {
    Surface(color = assistantCanvasColor()) {
        Column(modifier = Modifier.padding(16.dp)) {
            AssistantEarlyAccessNoticeCard(
                onFeedbackClick = {},
                onDismissClick = {},
            )
        }
    }
}
