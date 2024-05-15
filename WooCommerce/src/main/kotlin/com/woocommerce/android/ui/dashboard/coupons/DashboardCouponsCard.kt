package com.woocommerce.android.ui.dashboard.coupons

import androidx.compose.foundation.layout.Column
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.compose.viewModelWithFactory
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.WidgetCard
import com.woocommerce.android.ui.dashboard.WidgetError

@Composable
fun DashboardCouponsCard(
    parentViewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    viewModel: DashboardCouponsViewModel = viewModelWithFactory { factory: DashboardCouponsViewModel.Factory ->
        factory.create(parentViewModel)
    }
) {
    viewModel.viewState.observeAsState().value?.let { state ->
        DashboardCouponsCard(state, modifier)
    }
}

@Composable
private fun DashboardCouponsCard(
    viewState: DashboardCouponsViewModel.State,
    modifier: Modifier = Modifier
) {
    WidgetCard(
        titleResource = DashboardWidget.Type.COUPONS.titleResource,
        menu = DashboardWidgetMenu(emptyList()),
        isError = viewState is DashboardCouponsViewModel.State.Error,
        modifier = modifier
    ) {
        when (viewState) {
            is DashboardCouponsViewModel.State.Loading -> {
                CircularProgressIndicator()
            }

            is DashboardCouponsViewModel.State.Loaded -> {
                DashboardCouponsList(viewState)
            }

            is DashboardCouponsViewModel.State.Error -> {
                WidgetError(
                    onContactSupportClicked = { /*TODO*/ },
                    onRetryClicked = { /*TODO*/ }
                )
            }
        }
    }
}

@Composable
private fun DashboardCouponsList(
    state: DashboardCouponsViewModel.State.Loaded,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        state.coupons.forEach {
            Text(text = it.code)
            Divider()
        }
    }
}
