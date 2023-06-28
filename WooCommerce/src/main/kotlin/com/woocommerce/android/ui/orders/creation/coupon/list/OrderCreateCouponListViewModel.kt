package com.woocommerce.android.ui.orders.creation.coupon.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import javax.inject.Inject

class OrderCreateCouponListViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val navArgs: OrderCreateCouponListFragmentArgs by savedState.navArgs()

    val coupons: LiveData<List<Order.CouponLine>> = savedState.getStateFlow(
        viewModelScope,
        initialValue = navArgs.couponLines.asList(),
        "key_coupons"
    ).asLiveData()

    fun onAddCouponClicked() {
        triggerEvent(OrderCreateEditNavigationTarget.EditCoupon(null))
    }

    fun onCouponClicked(coupon: Order.CouponLine) {
        triggerEvent(OrderCreateEditNavigationTarget.EditCoupon(coupon.code))
    }

    fun onNavigateBack() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }
}
