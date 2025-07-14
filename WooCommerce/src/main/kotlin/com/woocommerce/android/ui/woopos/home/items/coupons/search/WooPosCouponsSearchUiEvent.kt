package com.woocommerce.android.ui.woopos.home.items.coupons.search

import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState

sealed class WooPosCouponsSearchUiEvent {
    data class OnCouponClicked(val coupon: WooPosItemSelectionViewState.Coupon) : WooPosCouponsSearchUiEvent()
    data object OnNextPageRequested : WooPosCouponsSearchUiEvent()
    data object LoadingErrorRetryButtonClicked : WooPosCouponsSearchUiEvent()
    data class OnRecentSearchClicked(val recentSearch: String) : WooPosCouponsSearchUiEvent()
}
