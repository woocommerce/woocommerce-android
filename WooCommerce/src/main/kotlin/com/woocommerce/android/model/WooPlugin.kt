package com.woocommerce.android.model

data class WooPlugin(
    val isInstalled: Boolean,
    val isActive: Boolean,
    val version: String?
)
