package com.woocommerce.android.ui.plans.domain

import java.time.LocalDate

sealed class FreeTrialExpiryDateResult {
    data class ExpiryAt(val date: LocalDate) : FreeTrialExpiryDateResult()
    object NotTrial : FreeTrialExpiryDateResult()
    data class Error(val message: String) : FreeTrialExpiryDateResult()
}
