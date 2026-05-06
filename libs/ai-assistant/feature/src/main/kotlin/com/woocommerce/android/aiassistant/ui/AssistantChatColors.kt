package com.woocommerce.android.aiassistant.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

internal fun ColorScheme.assistantInlineErrorTextColor(): Color = error

@Composable
internal fun assistantCanvasColor(): Color = MaterialTheme.colorScheme.surface

@Composable
internal fun assistantUserBubbleColor(): Color = MaterialTheme.colorScheme.primary

@Composable
internal fun assistantUserBubbleContentColor(): Color = MaterialTheme.colorScheme.onPrimary

@Composable
internal fun assistantBubbleColor(): Color {
    val colorScheme = MaterialTheme.colorScheme
    if (!isSystemInDarkTheme()) {
        return colorScheme.primary.copy(alpha = 0.06f).compositeOver(colorScheme.surface)
    }

    val darkBubble = colorScheme.surfaceContainerHighest
    return if (darkBubble == colorScheme.surface) {
        Color(0xFF1F1F1F)
    } else {
        darkBubble
    }
}

@Composable
internal fun assistantBubbleContentColor(): Color = MaterialTheme.colorScheme.onSurface

@Composable
internal fun assistantOutlineColor(): Color = MaterialTheme.colorScheme.outlineVariant

@Composable
internal fun assistantStatusGreen(): Color = if (isSystemInDarkTheme()) {
    Color(0xFF66BB6A)
} else {
    Color(0xFF2E7D32)
}
