package com.woocommerce.android.ui.woopos.home.items.coupons

import com.woocommerce.android.ui.woopos.home.items.WooPosCouponsViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosCouponsViewState.Content
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState.None
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState.Enabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class WooPosCouponsListViewStateManager @Inject constructor() {
    val viewState: Flow<WooPosCouponsViewState> = flow {
        while (true) {
            emit(WooPosCouponsViewState.Loading())
            delay(3000)
            emit(WooPosCouponsViewState.Empty())
            delay(3000)
            emit(WooPosCouponsViewState.Error())
            delay(3000)
            emit(
                Content(
                    items = listOf(
                        WooPosItemSelectionViewState.Coupon(
                            id = 1,
                            name = "Coupon 1",
                            summary = "10% off everything",
                        ),
                    ),
                    paginationState = None,
                    pullToRefreshState = Enabled,
                )
            )
            delay(3000)
            emit(
                Content(
                    items = listOf(
                        WooPosItemSelectionViewState.Coupon(
                            id = 1,
                            name = "Coupon 1",
                            summary = "10% off everything",
                        ),
                    ),
                    paginationState = WooPosPaginationState.Loading,
                    pullToRefreshState = Enabled,
                )
            )
            delay(3000)
            emit(
                Content(
                    items = listOf(
                        WooPosItemSelectionViewState.Coupon(
                            id = 1,
                            name = "Coupon 1",
                            summary = "10% off everything",
                        ),
                    ),
                    paginationState = WooPosPaginationState.Error,
                    pullToRefreshState = Enabled,
                )
            )
            delay(3000)
        }
    }

    fun fetchCoupons() {
    }

    fun loadMore() {
    }
}
