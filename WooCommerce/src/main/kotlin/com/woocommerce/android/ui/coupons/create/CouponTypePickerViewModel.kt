package com.woocommerce.android.ui.coupons.create

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.ui.coupons.edit.EditCouponViewModel
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CouponTypePickerViewModel @Inject constructor(savedStateHandle: SavedStateHandle) :
    ScopedViewModel(savedStateHandle) {
    private val navArgs = CouponTypePickerFragmentArgs.fromSavedStateHandle(savedStateHandle)

    fun onPercentageDiscountClicked() {
        triggerEvent(NavigateToCouponEdit(EditCouponViewModel.Mode.Create(Coupon.Type.Percent), navArgs.isPOSMode))
    }

    fun onFixedCartDiscountClicked() {
        triggerEvent(NavigateToCouponEdit(EditCouponViewModel.Mode.Create(Coupon.Type.FixedCart), navArgs.isPOSMode))
    }

    fun onFixedProductDiscountClicked() {
        triggerEvent(NavigateToCouponEdit(EditCouponViewModel.Mode.Create(Coupon.Type.FixedProduct), navArgs.isPOSMode))
    }

    fun onCancel() {
        if (navArgs.isPOSMode) {
            triggerEvent(NavigateBackToPOS)
        }
    }

    data class NavigateToCouponEdit(val mode: EditCouponViewModel.Mode.Create, val isPOSMode: Boolean) :
        MultiLiveEvent.Event()
    data object NavigateBackToPOS : MultiLiveEvent.Event()
}
