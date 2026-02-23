package com.woocommerce.android.ui.woopos.paymentsuccess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.cardpayment.WooPosCardPaymentAnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosPaymentSuccessViewModel @Inject constructor(
    private val analyticsTracker: WooPosCardPaymentAnalyticsTracker,
) : ViewModel() {
    fun onEmailReceiptClicked() {
        viewModelScope.launch {
            analyticsTracker.trackEmailReceiptTapped()
        }
    }
}
