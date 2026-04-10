package com.woocommerce.android.ui.troubleshooting.useCases

fun formatErrorDetails(
    operation: String,
    errorType: String,
    message: String?
): String = buildString {
    appendLine("Operation: $operation")
    appendLine("Error Type: $errorType")
    if (!message.isNullOrBlank()) {
        appendLine("Description: $message")
    }
}.trimEnd()
