package com.woocommerce.android.ui.login.storecreation.iapeligibility

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.iap.pub.PurchaseWPComPlanActions
import com.woocommerce.android.iap.pub.model.IAPSupportedResult
import com.woocommerce.android.ui.login.storecreation.iapeligibility.IapEligibilityViewModel.IapEligibilityEvent.NavigateToNativeStoreCreation
import com.woocommerce.android.ui.login.storecreation.iapeligibility.IapEligibilityViewModel.IapEligibilityEvent.NavigateToWebStoreCreation
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IapEligibilityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    purchaseWPComPlanActions: PurchaseWPComPlanActions
) : ScopedViewModel(savedStateHandle) {
    init {
        launch {
            purchaseWPComPlanActions.isIAPSupported().let {
                val event = when (it) {
                    is IAPSupportedResult.Error -> NavigateToWebStoreCreation
                    is IAPSupportedResult.Success -> NavigateToNativeStoreCreation
                }
                triggerEvent(event)
            }
        }
    }

    sealed class IapEligibilityEvent : MultiLiveEvent.Event() {
        object NavigateToNativeStoreCreation : IapEligibilityEvent()
        object NavigateToWebStoreCreation : IapEligibilityEvent()
    }
}
