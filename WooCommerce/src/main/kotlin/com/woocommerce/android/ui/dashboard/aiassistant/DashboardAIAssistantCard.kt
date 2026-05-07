package com.woocommerce.android.ui.dashboard.aiassistant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun DashboardAIAssistantCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentDescription = stringResource(R.string.dashboard_ai_assistant_entry_point_content_description)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = contentDescription }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = stringResource(R.string.dashboard_ai_assistant_entry_point_label),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@LightDarkThemePreviews
@Composable
private fun DashboardAIAssistantCardPreview() {
    WooThemeWithBackground {
        DashboardAIAssistantCard(onClick = {})
    }
}
