package com.woocommerce.android.aiassistant.core.loop

sealed interface RetryAffordance {
    data object None : RetryAffordance
    data object Manual : RetryAffordance
}
