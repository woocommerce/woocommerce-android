package com.woocommerce.android.ui.plans.domain

import java.time.ZonedDateTime

sealed class FreeTrialExpiryDateResult {
    data class ExpiryAt(val date: ZonedDateTime) : FreeTrialExpiryDateResult()
    object NotTrial : FreeTrialExpiryDateResult()
    data class Error(val message: String) : FreeTrialExpiryDateResult()
}
