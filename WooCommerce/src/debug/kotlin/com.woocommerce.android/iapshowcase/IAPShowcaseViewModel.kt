package com.woocommerce.android.iapshowcase

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.iap.pub.IAPSitePurchasePlanManager
import com.woocommerce.android.iap.pub.model.IAPProduct
import com.woocommerce.android.iap.pub.model.IAPProductInfo
import com.woocommerce.android.iap.pub.model.IAPProductInfoResponse
import com.woocommerce.android.iap.pub.model.IAPPurchaseResponse
import kotlinx.coroutines.launch

private val iapProductToBuy = IAPProduct.WPPremiumPlanTesting

class IAPShowcaseViewModel(private val iapManager: IAPSitePurchasePlanManager) : ViewModel() {
    private val _productInfo = MutableLiveData<IAPProductInfo>()
    val productInfo: LiveData<IAPProductInfo> = _productInfo

    private val _purchaseStatusInfo = MutableLiveData<String>()
    val purchaseStatusInfo: LiveData<String> = _purchaseStatusInfo

    private val _productInfoFetchingError = MutableLiveData<String>()
    val productInfoFetchingError: LiveData<String> = _productInfoFetchingError

    init {
        fetchProductAndPurchaseInfo()
    }

    fun fetchProductAndPurchaseInfo() {
        fetchProductInfo()
        fetchPurchases()
    }

    fun purchasePlan() {
        viewModelScope.launch {
            when (val response = iapManager.purchasePlan(iapProductToBuy)) {
                is IAPPurchaseResponse.Success -> {
                    val purchase = response.purchases!!.first()
                    Log.d(
                        "IAP",
                        "Info:\n" +
                            "Token: ${purchase.purchaseToken}\n" +
                            "Payload: ${purchase.developerPayload}\n" +
                            "Signature: ${purchase.signature}\n" +
                            "Order Id: ${purchase.orderId}\n" +
                            "State: ${purchase.state}\n" +
                            "Is acknowledged: ${purchase.isAcknowledged}\n" +
                            "Is AutoRenewing: ${purchase.isAutoRenewing}\n" +
                            "Products: ${purchase.products.joinToString { ", " }}"
                    )
                    _purchaseStatusInfo.value = "Plan ${iapProductToBuy.productId} successfully purchased"
                }
                is IAPPurchaseResponse.Error -> _purchaseStatusInfo.value = response.errorType.debugMessage
            }
        }
    }

    fun fetchPurchases() {
        viewModelScope.launch {
            if (iapManager.isPlanPurchased(iapProductToBuy)) {
                _purchaseStatusInfo.value = "Plan ${iapProductToBuy.productId} is already purchased"
            } else {
                _purchaseStatusInfo.value = "Plan ${iapProductToBuy.productId} is not purchased"
            }
        }
    }

    private fun fetchProductInfo() {
        viewModelScope.launch {
            when (val response = iapManager.fetchIapProductInfo(iapProductToBuy)) {
                is IAPProductInfoResponse.Success -> _productInfo.value = response.productInfo
                is IAPProductInfoResponse.Error -> _productInfoFetchingError.value = response.errorType.debugMessage
            }
        }
    }

    class Factory(private val iapManager: IAPSitePurchasePlanManager) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IAPShowcaseViewModel(iapManager) as T
        }
    }
}
