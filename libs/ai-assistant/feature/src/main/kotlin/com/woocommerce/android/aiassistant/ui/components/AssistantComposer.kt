package com.woocommerce.android.aiassistant.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.aiassistant.R

@Composable
internal fun AssistantComposer(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    isTurnActive: Boolean,
    shouldShowStopControl: Boolean,
    onSendMessage: () -> Unit,
    onCancelTurn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canSend = inputText.isNotBlank() && !isTurnActive
    val showPendingHint = isTurnActive && !shouldShowStopControl
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 4.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showPendingHint) {
                Text(
                    text = stringResource(R.string.assistant_chat_pending_confirmation_hint),
                    modifier = Modifier.padding(horizontal = 14.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = COMPOSER_MIN_HEIGHT),
                shape = RoundedCornerShape(COMPOSER_CORNER_RADIUS),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AssistantComposerInput(
                        inputText = inputText,
                        onInputTextChange = onInputTextChange,
                        canSend = canSend,
                        onSendMessage = onSendMessage,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 18.dp, top = 12.dp, bottom = 12.dp),
                    )
                    AssistantComposerActionButton(
                        shouldShowStopControl = shouldShowStopControl,
                        canSend = canSend,
                        onSendMessage = onSendMessage,
                        onCancelTurn = onCancelTurn,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantComposerInput(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    canSend: Boolean,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    BasicTextField(
        value = inputText,
        onValueChange = onInputTextChange,
        modifier = modifier,
        minLines = 1,
        maxLines = 6,
        textStyle = MaterialTheme.typography.bodyMedium.merge(TextStyle(color = textColor)),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(
            onSend = {
                if (canSend) {
                    onSendMessage()
                }
            }
        ),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (inputText.isEmpty()) {
                    Text(
                        text = stringResource(R.string.assistant_chat_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun AssistantComposerActionButton(
    shouldShowStopControl: Boolean,
    canSend: Boolean,
    onSendMessage: () -> Unit,
    onCancelTurn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonEnabled = shouldShowStopControl || canSend
    val buttonContainer = if (buttonEnabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val buttonContent = if (buttonEnabled) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val contentDescription = if (shouldShowStopControl) {
        stringResource(R.string.assistant_chat_stop_content_description)
    } else {
        stringResource(R.string.assistant_chat_send_content_description)
    }
    val iconRes = if (shouldShowStopControl) {
        R.drawable.ic_assistant_composer_stop
    } else {
        R.drawable.ic_assistant_composer_send
    }

    IconButton(
        onClick = {
            if (shouldShowStopControl) {
                onCancelTurn()
            } else if (canSend) {
                onSendMessage()
            }
        },
        enabled = buttonEnabled,
        modifier = modifier.size(COMPOSER_ACTION_HIT_TARGET_SIZE),
    ) {
        Surface(
            shape = CircleShape,
            color = buttonContainer,
            modifier = Modifier.size(COMPOSER_ACTION_BUTTON_SIZE),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = contentDescription,
                    tint = buttonContent,
                    modifier = Modifier.size(COMPOSER_ACTION_ICON_SIZE),
                )
            }
        }
    }
}

private val COMPOSER_MIN_HEIGHT = 56.dp
private val COMPOSER_CORNER_RADIUS = 28.dp
private val COMPOSER_ACTION_BUTTON_SIZE = 36.dp
private val COMPOSER_ACTION_HIT_TARGET_SIZE = 44.dp
private val COMPOSER_ACTION_ICON_SIZE = 18.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 104)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 104, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AssistantComposerReadyPreview() {
    AssistantComposer(
        inputText = "Show orders from today",
        onInputTextChange = {},
        isTurnActive = false,
        shouldShowStopControl = false,
        onSendMessage = {},
        onCancelTurn = {},
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 128)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 128, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AssistantComposerMultilinePreview() {
    AssistantComposer(
        inputText = "Find processing orders from this week and summarize the customer notes that mention shipping",
        onInputTextChange = {},
        isTurnActive = false,
        shouldShowStopControl = false,
        onSendMessage = {},
        onCancelTurn = {},
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 104)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 104, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AssistantComposerStreamingPreview() {
    AssistantComposer(
        inputText = "",
        onInputTextChange = {},
        isTurnActive = true,
        shouldShowStopControl = true,
        onSendMessage = {},
        onCancelTurn = {},
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 128)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 128, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AssistantComposerPendingConfirmationPreview() {
    AssistantComposer(
        inputText = "",
        onInputTextChange = {},
        isTurnActive = true,
        shouldShowStopControl = false,
        onSendMessage = {},
        onCancelTurn = {},
    )
}
