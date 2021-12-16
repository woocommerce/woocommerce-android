package com.woocommerce.android.ui.analytics.listcard

data class AnalyticsListCardItemViewState(
    val imageUri: String?,
    val title: String,
    val value: String,
    val description: String,
    val showDivider: Boolean = true
)
