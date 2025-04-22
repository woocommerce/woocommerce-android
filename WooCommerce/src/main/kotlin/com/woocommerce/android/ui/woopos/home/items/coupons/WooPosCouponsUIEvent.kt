package com.woocommerce.android.ui.woopos.home.items.coupons

sealed class WooPosCouponsUIEvent {
    data object EndOfItemsListReached : WooPosCouponsUIEvent()
    data object PullToRefreshTriggered : WooPosCouponsUIEvent()
    data object RetryLoadMoreTriggered : WooPosCouponsUIEvent()
    data class CouponClicked(val couponId: Long) : WooPosCouponsUIEvent()
    data object BackButtonClicked : WooPosCouponsUIEvent()
}
