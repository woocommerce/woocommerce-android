package com.woocommerce.android.aiassistant.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.aiassistant.ui.AssistantToolActivity
import com.woocommerce.android.aiassistant.ui.labelRes

/**
 * In-thread affordance announcing the tool the assistant is running. Reads as a pill but uses a
 * 12dp rounded rectangle to match the iOS reference. The leading affordance is the same animated
 * three-dot pulse used by [AssistantTypingIndicator] so the pill carries the same heartbeat once
 * typing dots hand off to a tool call.
 */
@Composable
internal fun AssistantToolActivityPill(
    activity: AssistantToolActivity,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(activity.labelRes())
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssistantInFlightDots(
                color = MaterialTheme.colorScheme.primary,
                dotSize = InFlightDotsToolPillSize,
                spacing = InFlightDotsToolPillSpacing,
            )
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 240, heightDp = 60)
@Preview(name = "Dark", showBackground = true, widthDp = 240, heightDp = 60, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AssistantToolActivityPillPreview() {
    AssistantToolActivityPill(
        activity = AssistantToolActivity(
            toolCallId = "call-preview",
            toolName = "orders_list",
        ),
        modifier = Modifier.padding(16.dp),
    )
}
