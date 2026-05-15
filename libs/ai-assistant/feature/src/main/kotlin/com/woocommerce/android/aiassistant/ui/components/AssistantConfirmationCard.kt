package com.woocommerce.android.aiassistant.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.safety.ConfirmationBulkEntry
import com.woocommerce.android.aiassistant.safety.RenderedConfirmationDiffRow
import com.woocommerce.android.aiassistant.safety.RenderedConfirmationPreview
import com.woocommerce.android.aiassistant.safety.RenderedConfirmationPreviewField
import com.woocommerce.android.aiassistant.ui.AssistantConfirmationCard
import com.woocommerce.android.aiassistant.ui.AssistantConfirmationCardState
import com.woocommerce.android.aiassistant.ui.eyebrowRes
import com.woocommerce.android.aiassistant.ui.iconRes
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ConfirmationCardEyebrow(state = confirmation.state, colors = colors)
            Text(
                text = confirmation.preview?.summary
                    ?: stringResource(R.string.ai_assistant_chat_confirm_tool, confirmation.toolCall.name),
                color = colors.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            confirmation.preview?.let { preview ->
                if (preview.isBulk && preview.bulkEntries.isNotEmpty()) {
                    ConfirmationBulkEntriesSection(
                        entries = preview.bulkEntries,
                        colors = colors,
                    )
                }
            }
            confirmation.preview?.let { preview ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    preview.rows.forEach { row ->
                        ConfirmationDiffRow(
                            row = row,
                            isBulk = preview.isBulk,
                            colors = colors,
                        )
                    }
                }
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
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(colors.accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(state.iconRes()),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = Color.White,
            )
        }
        Text(
            text = stringResource(state.eyebrowRes()).uppercase(),
            color = colors.accent,
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
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
                .weight(1f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, colors.border),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.ai_assistant_chat_cancel),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Button(
            onClick = onConfirmWrite,
            modifier = Modifier
                .weight(1f),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.ai_assistant_chat_confirm),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ConfirmationBulkEntriesSection(
    entries: List<ConfirmationBulkEntry>,
    colors: AssistantConfirmationCardColors,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val visible = entries.take(MAX_BULK_ENTRIES_VISIBLE)
        visible.forEach {
            Text(
                text = "#${it.id}",
                color = colors.value,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        val overflow = entries.size - visible.size
        if (overflow > 0) {
            Text(
                text = stringResource(R.string.ai_assistant_confirmation_bulk_entries_overflow, overflow),
                color = colors.label,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfirmationDiffRow(
    row: RenderedConfirmationPreviewField,
    isBulk: Boolean,
    colors: AssistantConfirmationCardColors,
) {
    val beforeValue = row.beforeValue.takeUnless { isBulk }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ConfirmationDiffText(
            text = "${row.label}:",
            color = colors.label,
            fontWeight = FontWeight.SemiBold,
        )
        beforeValue?.let {
            ConfirmationDiffText(
                text = it,
                color = colors.label,
                textDecoration = TextDecoration.LineThrough,
            )
        }
        ConfirmationDiffText(
            text = row.afterValue,
            color = colors.value,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ConfirmationDiffText(
    text: String,
    color: Color,
    fontWeight: FontWeight = FontWeight.Normal,
    textDecoration: TextDecoration? = null,
) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = fontWeight,
        textDecoration = textDecoration,
    )
}

@Composable
private fun AssistantConfirmationCardState.confirmationCardColors(): AssistantConfirmationCardColors {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = isSystemInDarkTheme()

    return when (this) {
        AssistantConfirmationCardState.PENDING -> AssistantConfirmationCardColors(
            container = colorScheme.surfaceContainer,
            border = colorScheme.outlineVariant,
            accent = if (isDarkTheme) Color(0xFFFFBF86) else Color(0xFFE68B28),
            title = colorScheme.onSurface,
            label = colorScheme.onSurfaceVariant,
            value = colorScheme.onSurface,
        )
        AssistantConfirmationCardState.CONFIRMED -> AssistantConfirmationCardColors(
            container = colorScheme.surfaceContainer,
            border = colorScheme.outlineVariant,
            accent = if (isDarkTheme) Color(0xFF1ED15A) else Color(0xFF008A20),
            title = colorScheme.onSurface,
            label = colorScheme.onSurfaceVariant,
            value = colorScheme.onSurface,
        )
        AssistantConfirmationCardState.CANCELLED -> AssistantConfirmationCardColors(
            container = colorScheme.surfaceContainer,
            border = colorScheme.outlineVariant,
            accent = if (isDarkTheme) Color(0xFFA7AAAD) else Color(0xFF787C82),
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

@Preview(showBackground = true, widthDp = 390, heightDp = 230)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 230, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AssistantConfirmationCardPendingPreview() {
    AssistantConfirmationCardPreviewContainer {
        AssistantConfirmationCardSegment(
            confirmation = sampleAssistantConfirmationCard(AssistantConfirmationCardState.PENDING),
            onConfirmWrite = {},
            onCancelWrite = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 176)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 176, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AssistantConfirmationCardConfirmedPreview() {
    AssistantConfirmationCardPreviewContainer {
        AssistantConfirmationCardSegment(
            confirmation = sampleAssistantConfirmationCard(AssistantConfirmationCardState.CONFIRMED),
            onConfirmWrite = {},
            onCancelWrite = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 224)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 224, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AssistantConfirmationCardBulkCancelledPreview() {
    AssistantConfirmationCardPreviewContainer {
        AssistantConfirmationCardSegment(
            confirmation = sampleAssistantConfirmationCard(
                state = AssistantConfirmationCardState.CANCELLED,
                isBulk = true,
            ),
            onConfirmWrite = {},
            onCancelWrite = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 306)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 306, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AssistantConfirmationCardBulkOverflowPreview() {
    AssistantConfirmationCardPreviewContainer {
        AssistantConfirmationCardSegment(
            confirmation = sampleAssistantConfirmationCard(
                state = AssistantConfirmationCardState.PENDING,
                isBulk = true,
                bulkIds = PREVIEW_BULK_OVERFLOW_ORDER_IDS,
            ),
            onConfirmWrite = {},
            onCancelWrite = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 284)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 284, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AssistantConfirmationCardBillingEmailPreview() {
    AssistantConfirmationCardPreviewContainer {
        AssistantConfirmationCardSegment(
            confirmation = sampleBillingEmailConfirmationCard(),
            onConfirmWrite = {},
            onCancelWrite = {},
        )
    }
}

@Composable
private fun AssistantConfirmationCardPreviewContainer(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                content()
            }
        }
    }
}

private fun sampleAssistantConfirmationCard(
    state: AssistantConfirmationCardState,
    isBulk: Boolean = false,
    bulkIds: List<Long> = PREVIEW_BULK_ORDER_IDS,
) = AssistantConfirmationCard(
    confirmationId = "confirmation-preview",
    toolCall = ToolCall(
        id = "call-preview",
        name = if (isBulk) "orders_bulk_update" else "orders_update",
        arguments = buildJsonObject {
            if (isBulk) {
                put("status", "completed")
            } else {
                put("id", PREVIEW_ORDER_ID)
                put("status", "completed")
            }
        },
    ),
    state = state,
    preview = if (isBulk) {
        RenderedConfirmationPreview(
            message = "Update 3 orders",
            fields = listOf(
                RenderedConfirmationDiffRow(
                    name = "status",
                    label = "Status",
                    value = "Completed",
                )
            ),
            isBulk = true,
            bulkEntries = bulkIds.map { ConfirmationBulkEntry(it) },
        )
    } else {
        RenderedConfirmationPreview(
            message = "Update order #3479",
            fields = listOf(
                RenderedConfirmationDiffRow(
                    name = "status",
                    label = "Status",
                    value = "Completed",
                    beforeValue = "Processing",
                )
            ),
            isBulk = false,
        )
    },
)

private fun sampleBillingEmailConfirmationCard() = AssistantConfirmationCard(
    confirmationId = "billing-email-confirmation-preview",
    toolCall = ToolCall(
        id = "call-billing-email-preview",
        name = "orders_update",
        arguments = buildJsonObject {
            put("id", PREVIEW_ORDER_ID)
            put("billing_email", PREVIEW_BILLING_EMAIL_AFTER)
        },
    ),
    state = AssistantConfirmationCardState.PENDING,
    preview = RenderedConfirmationPreview(
        message = "Update order #3650",
        fields = listOf(
            RenderedConfirmationDiffRow(
                name = BILLING_EMAIL_FIELD_NAME,
                label = "Billing email",
                value = PREVIEW_BILLING_EMAIL_AFTER,
                beforeValue = PREVIEW_BILLING_EMAIL_BEFORE,
            )
        ),
        isBulk = false,
    ),
)

private const val MAX_BULK_ENTRIES_VISIBLE = 5
private const val PREVIEW_ORDER_ID = 3479L
private const val PREVIEW_ORDER_ID_2 = 3480L
private const val PREVIEW_ORDER_ID_3 = 3481L
private const val PREVIEW_ORDER_ID_4 = 3482L
private const val PREVIEW_ORDER_ID_5 = 3483L
private const val PREVIEW_ORDER_ID_6 = 3484L
private const val PREVIEW_ORDER_ID_7 = 3485L
private const val PREVIEW_ORDER_ID_8 = 3486L
private val PREVIEW_BULK_ORDER_IDS = listOf(PREVIEW_ORDER_ID, PREVIEW_ORDER_ID_2, PREVIEW_ORDER_ID_3)
private val PREVIEW_BULK_OVERFLOW_ORDER_IDS = listOf(
    PREVIEW_ORDER_ID,
    PREVIEW_ORDER_ID_2,
    PREVIEW_ORDER_ID_3,
    PREVIEW_ORDER_ID_4,
    PREVIEW_ORDER_ID_5,
    PREVIEW_ORDER_ID_6,
    PREVIEW_ORDER_ID_7,
    PREVIEW_ORDER_ID_8,
)
private const val BILLING_EMAIL_FIELD_NAME = "billing_email"
private const val PREVIEW_BILLING_EMAIL_BEFORE = "schuster.alden@schuster-fulfillment.example.com"
private const val PREVIEW_BILLING_EMAIL_AFTER = "alexandra.merchant@northstar-woocommerce.example.com"
