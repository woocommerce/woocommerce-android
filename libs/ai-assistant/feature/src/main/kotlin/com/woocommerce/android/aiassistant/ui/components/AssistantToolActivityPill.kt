package com.woocommerce.android.aiassistant.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.ui.AssistantToolActivity
import com.woocommerce.android.aiassistant.ui.assistantOutlineColor
import com.woocommerce.android.aiassistant.ui.assistantStatusGreen
import com.woocommerce.android.aiassistant.ui.labelRes

/**
 * In-thread affordance announcing the tool the assistant is running. Leading affordance is the
 * animated three-dot pulse (matches [AssistantTypingIndicator]) while the tool runs and a static
 * checkmark once the tool finishes — completed activities are preserved in the thread as a step
 * history.
 */
@Composable
internal fun AssistantToolActivityPill(
    activity: AssistantToolActivity,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(activity.labelRes())
    val description = stringResource(R.string.assistant_chat_tool_activity_content_description, label)
    Surface(
        modifier = modifier
            .animateContentSize(animationSpec = tween(durationMillis = 220))
            .semantics {
                contentDescription = description
                if (activity.status == AssistantToolActivity.Status.RUNNING) {
                    liveRegion = LiveRegionMode.Polite
                }
            },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, assistantOutlineColor()),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolActivityLeadingAffordance(status = activity.status)
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun ToolActivityLeadingAffordance(status: AssistantToolActivity.Status) {
    when (status) {
        AssistantToolActivity.Status.RUNNING -> AssistantInFlightDots(
            color = assistantStatusGreen(),
            dotSize = InFlightDotsToolPillSize,
            spacing = InFlightDotsToolPillSpacing,
        )
        AssistantToolActivity.Status.COMPLETED -> Icon(
            painter = painterResource(R.drawable.ic_assistant_tool_completed),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = assistantStatusGreen(),
        )
    }
}

@Preview(showBackground = true, widthDp = 240, heightDp = 60)
@Preview(name = "Dark", showBackground = true, widthDp = 240, heightDp = 60, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AssistantToolActivityPillRunningPreview() {
    AssistantToolActivityPill(
        activity = AssistantToolActivity(
            toolCallId = "call-preview",
            toolName = "orders_list",
            status = AssistantToolActivity.Status.RUNNING,
        ),
        modifier = Modifier.padding(16.dp),
    )
}

@Preview(showBackground = true, widthDp = 240, heightDp = 60)
@Preview(name = "Dark", showBackground = true, widthDp = 240, heightDp = 60, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AssistantToolActivityPillCompletedPreview() {
    AssistantToolActivityPill(
        activity = AssistantToolActivity(
            toolCallId = "call-preview",
            toolName = "orders_list",
            status = AssistantToolActivity.Status.COMPLETED,
        ),
        modifier = Modifier.padding(16.dp),
    )
}
