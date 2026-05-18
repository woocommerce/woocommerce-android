package com.woocommerce.android.aiassistant.headless

import java.io.File

data class WooAiSmokeCredentialConfig(
    val siteUrl: String,
    val siteId: Long,
    val username: String,
    val appPassword: String,
    val mode: WooAiSmokeBaselineMode,
    val storeLabel: String,
    val outputDirectory: File,
    val credentialSource: String,
)
