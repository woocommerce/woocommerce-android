package com.woocommerce.android.iapshowcase.supportcheck

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.iap.pub.PurchaseWpComPlanSupportChecker
import com.woocommerce.android.iap.pub.model.IAPError
import com.woocommerce.android.iap.pub.model.IAPSupportedResult
import com.woocommerce.android.viewmodel.SingleLiveEvent
import kotlinx.coroutines.launch

class IAPShowcaseSupportCheckerViewModel(
    private val planSupportChecker: PurchaseWpComPlanSupportChecker
) : ViewModel(planSupportChecker) {
    private val _iapEvent = SingleLiveEvent<String>()
    val iapEvent: LiveData<String> = _iapEvent

    private val _iapLoading = SingleLiveEvent<Boolean>()
    val iapLoading: LiveData<Boolean> = _iapLoading

    fun checkIfIAPSupported() {
        viewModelScope.launch {
            _iapLoading.value = true
            val response = planSupportChecker.isIAPSupported()
            _iapLoading.value = false
            when (response) {
                is IAPSupportedResult.Success -> {
                    _iapEvent.value = if (response.isSupported) {
                        "IAP is supported"
                    } else {
                        "IAP is not supported"
                    }
                }
                is IAPSupportedResult.Error -> handleError(response.errorType)
            }
        }
    }

    private fun handleError(error: IAPError) {
        _iapEvent.value = when (error) {
            is IAPError.Billing -> "Billing error: ${error::class.java.simpleName} ${error.debugMessage}"
            IAPError.RemoteCommunication.Network -> "Network error"
            is IAPError.RemoteCommunication.Server -> "Server error: ${error.reason}"
        }
    }
}
