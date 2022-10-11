package com.woocommerce.android.iapshowcase

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.iap.public.IAPSitePurchasePlanManager
import com.woocommerce.android.iap.public.model.IAPProduct
import com.woocommerce.android.iap.public.model.IAPProductInfo
import com.woocommerce.android.iap.public.model.IAPProductInfoResponse
import com.woocommerce.android.iap.public.model.IAPPurchaseResponse
import kotlinx.coroutines.launch

private val iapProductToBuy = IAPProduct.WPPremiumPlanTesting

class IAPShowcaseViewModel(private val iapManager: IAPSitePurchasePlanManager) : ViewModel() {
    private val _productInfo = MutableLiveData<IAPProductInfo>()
    val productInfo: LiveData<IAPProductInfo> = _productInfo

    private val _purchaseStatusInfo = MutableLiveData<String>()
    val purchaseStatusInfo: LiveData<String> = _purchaseStatusInfo

    init {
        viewModelScope.launch {
            when (val response = iapManager.fetchIapProductInfo(iapProductToBuy)) {
                is IAPProductInfoResponse.Success -> _productInfo.value = response.productInfo
                is IAPProductInfoResponse.Error -> _purchaseStatusInfo.value = response.errorType.debugMessage
            }
        }
    }

    fun purchasePlan() {
        viewModelScope.launch {
            when (val response = iapManager.purchasePlan(iapProductToBuy)) {
                is IAPPurchaseResponse.Success -> {
                    val purchase = response.purchases!!.first()
                    _purchaseStatusInfo.value = "Plan ${iapProductToBuy.productId} successfully purchased." +
                        "Info:\n" +
                        "Token: ${purchase.purchaseToken}\n" +
                        "Payload: ${purchase.developerPayload}\n" +
                        "Signature: ${purchase.signature}\n" +
                        "Order Id: ${purchase.orderId}\n" +
                        "State: ${purchase.state}\n" +
                        "Products: ${purchase.products.joinToString { ", " }}"
                }
                is IAPPurchaseResponse.Error -> _purchaseStatusInfo.value = response.errorType.debugMessage
            }
        }
    }

    fun fetchPurchases() {
        viewModelScope.launch {
            if (iapManager.isPlanPurchased(iapProductToBuy)) {
                _purchaseStatusInfo.value = "Plan ${iapProductToBuy.productId} is already purchased"
            }
        }
    }

    class Factory(private val iapManager: IAPSitePurchasePlanManager) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IAPShowcaseViewModel(iapManager) as T
        }
    }
}
