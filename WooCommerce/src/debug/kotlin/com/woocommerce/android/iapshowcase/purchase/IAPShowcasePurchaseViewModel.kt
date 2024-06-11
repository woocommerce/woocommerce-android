package com.woocommerce.android.iapshowcase.purchase

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.iap.pub.IAPActivityWrapper
import com.woocommerce.android.iap.pub.PurchaseWPComPlanActions
import com.woocommerce.android.iap.pub.model.IAPError
import com.woocommerce.android.iap.pub.model.PurchaseStatus
import com.woocommerce.android.iap.pub.model.WPComIsPurchasedResult
import com.woocommerce.android.iap.pub.model.WPComPlanProduct
import com.woocommerce.android.iap.pub.model.WPComProductResult
import com.woocommerce.android.iap.pub.model.WPComPurchaseResult
import com.woocommerce.android.viewmodel.SingleLiveEvent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class IAPShowcasePurchaseViewModel(private val iapManager: PurchaseWPComPlanActions) : ViewModel(iapManager) {
    private companion object {
        const val REMOTE_SITE_ID = 1L
    }

    private val _productInfo = MutableLiveData<WPComPlanProduct>()
    val productInfo: LiveData<WPComPlanProduct> = _productInfo

    private val _iapEvent = SingleLiveEvent<String>()
    val iapEvent: LiveData<String> = _iapEvent

    private val _iapLoading = SingleLiveEvent<Boolean>()
    val iapLoading: LiveData<Boolean> = _iapLoading

    init {
        viewModelScope.launch {
            iapManager.getPurchaseWpComPlanResult(REMOTE_SITE_ID).collectLatest { result ->
                _iapLoading.value = false
                when (result) {
                    is WPComPurchaseResult.Success -> _iapEvent.value = "Plan has been successfully purchased"
                    is WPComPurchaseResult.Error -> handleError(result.errorType)
                }
            }
        }
    }

    fun purchasePlan(activityWrapper: IAPActivityWrapper) {
        viewModelScope.launch {
            _iapLoading.value = true
            iapManager.purchaseWPComPlan(activityWrapper, REMOTE_SITE_ID)
        }
    }

    fun checkIfWPComPlanPurchased() {
        viewModelScope.launch {
            _iapLoading.value = true
            val response = iapManager.isWPComPlanPurchased()
            _iapLoading.value = false

            when (response) {
                is WPComIsPurchasedResult.Success -> {
                    _iapEvent.value = when (response.purchaseStatus) {
                        PurchaseStatus.PURCHASED_AND_ACKNOWLEDGED -> "Plan has been purchased acknowledged already"
                        PurchaseStatus.PURCHASED -> "Plan has been purchased, but not acknowledged"
                        PurchaseStatus.NOT_PURCHASED -> "Plan hasn't been purchased yet"
                    }
                }

                is WPComIsPurchasedResult.Error -> handleError(response.errorType)
            }
        }
    }

    fun fetchWPComPlanProduct() {
        viewModelScope.launch {
            _iapLoading.value = true
            val response = iapManager.fetchWPComPlanProduct()
            _iapLoading.value = false
            when (response) {
                is WPComProductResult.Success -> _productInfo.value = response.productInfo
                is WPComProductResult.Error -> handleError(response.errorType)
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
