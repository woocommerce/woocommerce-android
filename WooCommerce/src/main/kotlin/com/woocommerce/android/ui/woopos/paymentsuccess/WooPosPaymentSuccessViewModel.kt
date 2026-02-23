package com.woocommerce.android.ui.woopos.paymentsuccess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosPaymentSuccessViewModel @Inject constructor(
    private val analyticsTracker: WooPosAnalyticsTracker,
) : ViewModel() {
    fun onEmailReceiptClicked() {
        viewModelScope.launch {
            analyticsTracker.track(WooPosAnalyticsEvent.Event.EmailReceiptTapped)
        }
    }
}