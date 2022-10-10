package com.woocommerce.android.iapshowcase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.iap.public.IAPSitePurchasePlanManager
import com.woocommerce.android.iap.public.model.IAPProduct
import kotlinx.coroutines.launch

class IAPShowcaseViewModel(private val iapManager: IAPSitePurchasePlanManager) : ViewModel() {
    init {
        viewModelScope.launch {
            iapManager.fetchIapProductInfo(IAPProduct.WPPremiumPlan)
        }
    }

    fun purchasePlan() {
        TODO("Not yet implemented")
    }

    fun fetchPurchases() {
        viewModelScope.launch {
            iapManager.isPlanPurchased(IAPProduct.WPPremiumPlan)
        }
    }

    class Factory(private val iapManager: IAPSitePurchasePlanManager) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IAPShowcaseViewModel(iapManager) as T
        }
    }
}
