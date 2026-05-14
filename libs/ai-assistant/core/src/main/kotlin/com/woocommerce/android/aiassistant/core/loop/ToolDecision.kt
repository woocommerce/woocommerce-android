package com.woocommerce.android.aiassistant.core.loop

enum class ToolDecision {
    EXECUTED,
    MALFORMED_ARGUMENTS,
    VALIDATION_FAILED,
    REJECTED_BY_SAFETY,
    HANDLER_FAILED,
    CAP_EXCEEDED,
    REPLAYED,
}
