package com.woocommerce.android.aiassistant.tools

internal data class TruncatedText(val value: String, val truncated: Boolean)

internal fun String.truncated(limit: Int): TruncatedText = TruncatedText(
    value = take(limit),
    truncated = length > limit,
)
