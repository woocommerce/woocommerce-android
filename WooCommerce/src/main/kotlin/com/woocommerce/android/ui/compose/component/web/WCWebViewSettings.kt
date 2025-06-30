package com.woocommerce.android.ui.compose.component.web

data class WCWebViewSettings(
    val loadWithOverviewMode: Boolean = false,
    val useWideViewPort: Boolean = false,
    val isJavaScriptEnabled: Boolean = true,
    val isDomStorageEnabled: Boolean = true,
    val isReadOnly: Boolean = false,
    val initialScale: Int = 0,
)
