package com.woocommerce.android.aiassistant.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal fun ColorScheme.assistantInlineErrorTextColor(): Color = error

@Composable
internal fun assistantCanvasColor(): Color = MaterialTheme.colorScheme.surface

@Composable
internal fun assistantUserBubbleColor(): Color = MaterialTheme.colorScheme.primary

@Composable
internal fun assistantUserBubbleContentColor(): Color = Color.White

@Composable
internal fun assistantOutlineColor(): Color = MaterialTheme.colorScheme.outlineVariant

@Composable
internal fun assistantStatusGreen(): Color = if (isSystemInDarkTheme()) {
    Color(0xFF66BB6A)
} else {
    Color(0xFF2E7D32)
}
