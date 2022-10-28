package com.woocommerce.android.iapshowcase

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.iap.internal.model.IAPSupportedResult
import com.woocommerce.android.iap.pub.PurchaseWPComPlanActions
import com.woocommerce.android.iap.pub.model.IAPError
import com.woocommerce.android.iap.pub.model.WPComIsPurchasedResult
import com.woocommerce.android.iap.pub.model.WPComPlanProduct
import com.woocommerce.android.iap.pub.model.WPComProductResult
import com.woocommerce.android.iap.pub.model.WPComPurchaseResult
import com.woocommerce.android.viewmodel.SingleLiveEvent
import kotlinx.coroutines.launch

class IAPShowcaseViewModel : ViewModel() {
    lateinit var iapManager: PurchaseWPComPlanActions

    private val _productInfo = MutableLiveData<WPComPlanProduct>()
    val productInfo: LiveData<WPComPlanProduct> = _productInfo

    private val _iapEvent = SingleLiveEvent<String>()
    val iapEvent: LiveData<String> = _iapEvent

    fun purchasePlan(remoteSiteId: Long) {
        viewModelScope.launch {
            when (val response = iapManager.purchaseWPComPlan(remoteSiteId)) {
                is WPComPurchaseResult.Success -> _iapEvent.value = "Plan has been successfully purchased"
                is WPComPurchaseResult.Error -> handleError(response.errorType)
            }
        }
    }

    fun checkIfWPComPlanPurchased() {
        viewModelScope.launch {
            when (val response = iapManager.isWPComPlanPurchased()) {
                is WPComIsPurchasedResult.Success -> {
                    _iapEvent.value = if (response.isPlanPurchased) {
                        "Plan has been purchased already"
                    } else {
                        "Plan hasn't been purchased yet"
                    }
                }
                is WPComIsPurchasedResult.Error -> handleError(response.errorType)
            }
        }
    }

    fun fetchWPComPlanProduct() {
        viewModelScope.launch {
            when (val response = iapManager.fetchWPComPlanProduct()) {
                is WPComProductResult.Success -> _productInfo.value = response.productInfo
                is WPComProductResult.Error -> handleError(response.errorType)
            }
        }
    }

    fun checkIfIAPSupported() {
        viewModelScope.launch {
            when (val response = iapManager.isIAPSupported()) {
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
