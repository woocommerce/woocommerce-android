package com.woocommerce.android.ui.analytics.hub.listcard

data class AnalyticsHubListCardItemViewState(
    val imageUri: String?,
    val title: String,
    val value: String,
    val description: String,
    val showDivider: Boolean = true,
    val showImage: Boolean = true
)
