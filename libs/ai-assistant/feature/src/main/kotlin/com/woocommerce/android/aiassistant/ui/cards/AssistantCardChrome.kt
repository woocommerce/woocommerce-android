package com.woocommerce.android.aiassistant.ui.cards

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.aiassistant.ui.assistantOutlineColor

@Composable
internal fun AssistantCardChrome(
    title: String,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIconRes: Int? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ASSISTANT_CARD_CORNER_RADIUS),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, assistantOutlineColor()),
        tonalElevation = 0.dp,
        shadowElevation = if (isSystemInDarkTheme()) 0.dp else 1.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leadingIconRes?.let { iconRes ->
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            HorizontalDivider(color = assistantOutlineColor().copy(alpha = 0.5f))
            content()
        }
    }
}

private val ASSISTANT_CARD_CORNER_RADIUS = 20.dp
