package com.woocommerce.wear.presentation.theme

import androidx.compose.material.Surface
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

@Composable
private fun SurfacedContent(
    content: @Composable () -> Unit
) {
    Surface(color = MaterialTheme.colors.background) {
        content()
    }
}
