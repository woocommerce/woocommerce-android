package com.woocommerce.android.ui.blaze

import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.fluxc.model.blaze.BlazeBillingSummary
import javax.inject.Inject

/**
 * Scoped to the main activity, which is finished on logout, so the balance of one account is never shown to the next.
 */
@ActivityRetainedScoped
class BlazeOutstandingBalanceCache @Inject constructor() {
    private val _outstandingBalance = MutableStateFlow<BlazeBillingSummary?>(null)
    val outstandingBalance: StateFlow<BlazeBillingSummary?> = _outstandingBalance.asStateFlow()

    fun update(outstandingBalance: BlazeBillingSummary?) {
        _outstandingBalance.value = outstandingBalance
    }
}
