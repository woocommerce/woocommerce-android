package com.woocommerce.android.ui.coupons.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState

@Composable
fun EditCouponScreen(viewModel: EditCouponViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        EditCouponScreen(it)
    }
}

@Composable
fun EditCouponScreen(viewState: EditCouponViewModel.ViewState) {
    /* TODO */
}
