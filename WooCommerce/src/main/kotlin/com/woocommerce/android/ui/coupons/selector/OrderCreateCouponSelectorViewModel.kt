package com.woocommerce.android.ui.coupons.selector

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OrderCreateCouponSelectorViewModel @Inject constructor(
    savedState: SavedStateHandle,
//    private val wooCommerceStore: WooCommerceStore,
//    private val selectedSite: SelectedSite,
//    private val couponListHandler: CouponListHandler,
//    private val couponUtils: CouponUtils,
) : ScopedViewModel(savedState) {

    // TODO Implement the ViewModel
}
