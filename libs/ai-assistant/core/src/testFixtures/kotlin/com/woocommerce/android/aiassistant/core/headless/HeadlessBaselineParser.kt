package com.woocommerce.android.aiassistant.core.headless

import kotlinx.serialization.json.Json

class HeadlessBaselineParser(
    private val json: Json,
) {
    fun parse(source: String): HeadlessBaseline =
        json.decodeFromString(HeadlessBaseline.serializer(), source)

    fun parseApprovedBaseline(source: String): HeadlessApprovedBaseline =
        json.decodeFromString(HeadlessApprovedBaseline.serializer(), source)
}
