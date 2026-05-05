package com.woocommerce.android.aiassistant.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    modifier = Modifier.weight(1f),
                    minLines = 1,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text(stringResource(R.string.assistant_chat_placeholder)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (canSend) {
                                onSendMessage()
                            }
                        }
                    ),
                )
                if (shouldShowStopControl) {
                    Button(
                        onClick = onCancelTurn,
                        modifier = Modifier.heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Text(stringResource(R.string.assistant_chat_stop))
                    }
                } else {
                    Button(
                        onClick = onSendMessage,
                        enabled = canSend,
                        modifier = Modifier.heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(stringResource(R.string.assistant_chat_send))
                    }
                }
            }
        }
    }
}

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
