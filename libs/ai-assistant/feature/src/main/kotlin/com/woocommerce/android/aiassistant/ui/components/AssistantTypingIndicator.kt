package com.woocommerce.android.aiassistant.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.aiassistant.R

/**
 * Pre-text "thinking" indicator. Lives at the bottom of the message thread while the assistant is
 * waiting on its first text token (matches iOS `streamingState == .sending`). A muted, fully-rounded
 * pill containing three softly pulsing dots.
 */
@Composable
internal fun AssistantTypingIndicator(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.ai_assistant_chat_typing_content_description)
    Surface(
        modifier = modifier.semantics {
            contentDescription = description
            liveRegion = LiveRegionMode.Polite
        },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        AssistantInFlightDots(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            dotSize = InFlightDotsTypingSize,
            spacing = InFlightDotsTypingSpacing,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 200, heightDp = 60)
@Preview(name = "Dark", showBackground = true, widthDp = 200, heightDp = 60, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AssistantTypingIndicatorPreview() {
    AssistantTypingIndicator(modifier = Modifier.padding(16.dp))
}
