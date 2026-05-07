package com.woocommerce.android.aiassistant.core.loop

import kotlinx.serialization.json.Json

internal fun assistantJsonForTests(): Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}
