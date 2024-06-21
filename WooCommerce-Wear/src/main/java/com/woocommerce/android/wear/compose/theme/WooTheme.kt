package com.woocommerce.android.wear.compose.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

@Composable
fun WooTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = WooWearColors,
        typography = WooTypography,
        content = content
    )
}
