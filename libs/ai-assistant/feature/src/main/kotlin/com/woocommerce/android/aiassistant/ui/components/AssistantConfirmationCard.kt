package com.woocommerce.android.aiassistant.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.safety.RenderedConfirmationPreviewField
import com.woocommerce.android.aiassistant.ui.AssistantConfirmationCard
import com.woocommerce.android.aiassistant.ui.AssistantConfirmationCardState
import com.woocommerce.android.aiassistant.ui.eyebrowRes
import com.woocommerce.android.aiassistant.ui.iconRes

@Composable
internal fun AssistantConfirmationCardSegment(
    confirmation: AssistantConfirmationCard,
    onConfirmWrite: () -> Unit,
    onCancelWrite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = confirmation.state.confirmationCardColors()
    val shape = RoundedCornerShape(12.dp)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = colors.container,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ConfirmationCardEyebrow(state = confirmation.state, colors = colors)
            Text(
                text = confirmation.preview?.summary
                    ?: stringResource(R.string.assistant_chat_confirm_tool, confirmation.toolCall.name),
                color = colors.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            confirmation.preview?.rows?.forEach { row ->
                ConfirmationDiffRow(
                    row = row,
                    isBulk = confirmation.preview.isBulk,
                    colors = colors,
                )
            }
            if (confirmation.state == AssistantConfirmationCardState.PENDING) {
                ConfirmationActions(
                    colors = colors,
                    onConfirmWrite = onConfirmWrite,
                    onCancelWrite = onCancelWrite,
                )
            }
        }
    }
}

@Composable
private fun ConfirmationCardEyebrow(
    state: AssistantConfirmationCardState,
    colors: AssistantConfirmationCardColors,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(state.iconRes()),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = colors.accent,
        )
        Text(
            text = stringResource(state.eyebrowRes()),
            color = colors.accent,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ConfirmationActions(
    colors: AssistantConfirmationCardColors,
    onConfirmWrite: () -> Unit,
    onCancelWrite: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onCancelWrite,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, colors.border),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.title),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.assistant_chat_cancel),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Button(
            onClick = onConfirmWrite,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.assistant_chat_confirm),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ConfirmationDiffRow(
    row: RenderedConfirmationPreviewField,
    isBulk: Boolean,
    colors: AssistantConfirmationCardColors,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .border(1.dp, colors.border.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = row.label,
            modifier = Modifier.weight(0.85f),
            color = colors.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.weight(1.15f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isBulk) {
                row.beforeValue?.let { beforeValue ->
                    ConfirmationDiffValue(
                        value = beforeValue,
                        colors = colors,
                        strikethrough = true,
                    )
                }
            }
            ConfirmationDiffValue(
                value = row.afterValue,
                colors = colors,
            )
        }
    }
}

@Composable
private fun ConfirmationDiffValue(
    value: String,
    colors: AssistantConfirmationCardColors,
    strikethrough: Boolean = false,
) {
    Text(
        text = value,
        color = if (strikethrough) colors.label else colors.value,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (strikethrough) FontWeight.Normal else FontWeight.SemiBold,
        textDecoration = if (strikethrough) TextDecoration.LineThrough else null,
    )
}

@Composable
private fun AssistantConfirmationCardState.confirmationCardColors(): AssistantConfirmationCardColors {
    val colorScheme = MaterialTheme.colorScheme

    return when (this) {
        AssistantConfirmationCardState.PENDING -> AssistantConfirmationCardColors(
            container = colorScheme.surfaceContainerHigh,
            border = colorScheme.primary.copy(alpha = 0.28f),
            accent = colorScheme.primary,
            title = colorScheme.onSurface,
            label = colorScheme.onSurfaceVariant,
            value = colorScheme.onSurface,
        )
        AssistantConfirmationCardState.CONFIRMED -> AssistantConfirmationCardColors(
            container = colorScheme.surfaceContainerHigh,
            border = colorScheme.outlineVariant,
            accent = colorScheme.primary,
            title = colorScheme.onSurface,
            label = colorScheme.onSurfaceVariant,
            value = colorScheme.onSurface,
        )
        AssistantConfirmationCardState.CANCELLED -> AssistantConfirmationCardColors(
            container = colorScheme.surfaceContainerLow,
            border = colorScheme.outlineVariant,
            accent = colorScheme.onSurfaceVariant,
            title = colorScheme.onSurfaceVariant,
            label = colorScheme.onSurfaceVariant,
            value = colorScheme.onSurfaceVariant,
        )
    }
}

private data class AssistantConfirmationCardColors(
    val container: Color,
    val border: Color,
    val accent: Color,
    val title: Color,
    val label: Color,
    val value: Color,
)
