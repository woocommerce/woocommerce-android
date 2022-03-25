package com.woocommerce.android.ui.coupons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.woocommerce.android.ui.coupons.CouponSummaryViewModel.CouponSummaryState

@Composable
fun CouponSummaryScreen(viewModel: CouponSummaryViewModel) {
    val couponSummaryState by viewModel.couponState.observeAsState(CouponSummaryState())

    CouponSummaryScreen(state = couponSummaryState)
}

@Composable
fun CouponSummaryScreen(state: CouponSummaryState) {
    // to do
}
