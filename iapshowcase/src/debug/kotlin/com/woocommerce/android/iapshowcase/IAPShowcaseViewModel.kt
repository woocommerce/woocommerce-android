package com.woocommerce.android.iapshowcase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.woocommerce.android.iap.public.IAPSitePurchasePlanManager

class IAPShowcaseViewModel(iapManager: IAPSitePurchasePlanManager) : ViewModel() {
    fun purchasePlan() {
    }

    fun fetchPurchases() {
    }

    class Factory(private val iapManager: IAPSitePurchasePlanManager) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IAPShowcaseViewModel(iapManager) as T
        }
    }
}
