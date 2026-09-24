package com.woocommerce.android.ui.blaze

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.fluxc.model.blaze.BlazeBillingSummary
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlazeOutstandingBalanceCache @Inject constructor() {
    private val _outstandingBalance = MutableStateFlow<BlazeBillingSummary?>(null)
    val outstandingBalance: StateFlow<BlazeBillingSummary?> = _outstandingBalance.asStateFlow()

    fun update(outstandingBalance: BlazeBillingSummary?) {
        _outstandingBalance.value = outstandingBalance
    }
}
