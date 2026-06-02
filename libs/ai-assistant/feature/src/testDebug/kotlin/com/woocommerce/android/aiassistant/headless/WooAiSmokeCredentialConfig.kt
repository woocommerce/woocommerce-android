package com.woocommerce.android.aiassistant.headless

import java.io.File

data class WooAiSmokeCredentialConfig(
    val siteUrl: String,
    val wpComUsername: String,
    val wpComPassword: String,
    val storeLabel: String,
    val outputDirectory: File,
    val credentialSource: String,
    val sampleCount: Int = 1,
    val scenarioIds: Set<String> = emptySet(),
)
