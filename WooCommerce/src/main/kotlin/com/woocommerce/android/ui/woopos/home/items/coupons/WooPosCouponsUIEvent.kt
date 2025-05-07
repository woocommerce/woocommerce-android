package com.woocommerce.android.ui.woopos.home.items.coupons

sealed class WooPosCouponsUIEvent {
    data object EndOfListReached : WooPosCouponsUIEvent()
    data object PullToRefreshTriggered : WooPosCouponsUIEvent()
    data object RetryLoadMoreTriggered : WooPosCouponsUIEvent()
    data object RetryTriggered : WooPosCouponsUIEvent()
    data class CouponClicked(val couponId: Long) : WooPosCouponsUIEvent()
    data object CreateCouponClicked : WooPosCouponsUIEvent()
    data object BackButtonClicked : WooPosCouponsUIEvent()
}
