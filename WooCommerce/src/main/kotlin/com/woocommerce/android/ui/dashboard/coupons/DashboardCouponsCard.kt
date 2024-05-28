package com.woocommerce.android.ui.dashboard.coupons

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.woocommerce.android.model.DashboardWidget.Type.COUPONS
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRange
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.compose.viewModelWithFactory
import com.woocommerce.android.ui.dashboard.DashboardDateRangeHeader
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.WidgetCard
import com.woocommerce.android.ui.dashboard.WidgetError
import com.woocommerce.android.ui.dashboard.coupons.DashboardCouponsViewModel.DateRangeState
import com.woocommerce.android.ui.dashboard.coupons.DashboardCouponsViewModel.State
import com.woocommerce.android.ui.dashboard.coupons.DashboardCouponsViewModel.State.Error
import com.woocommerce.android.ui.dashboard.coupons.DashboardCouponsViewModel.State.Loaded
import com.woocommerce.android.ui.dashboard.coupons.DashboardCouponsViewModel.State.Loading
import com.woocommerce.android.viewmodel.MultiLiveEvent
import java.util.Date

@Composable
fun DashboardCouponsCard(
    parentViewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    viewModel: DashboardCouponsViewModel = viewModelWithFactory { factory: DashboardCouponsViewModel.Factory ->
        factory.create(parentViewModel)
    }
) {
    val viewState = viewModel.viewState.observeAsState().value ?: return
    val dateRangeState = viewModel.dateRangeState.observeAsState().value ?: return

    HandleEvents(
        event = viewModel.event,
        openDatePicker = { fromDate, toDate ->
            parentViewModel.onDashboardWidgetEvent(
                DashboardViewModel.DashboardEvent.OpenRangePicker(fromDate, toDate) { from, to ->
                    viewModel.onCustomRangeSelected(StatsTimeRange(Date(from), Date(to)))
                }
            )
        }
    )

    DashboardCouponsCard(
        dateRangeState = dateRangeState,
        viewState = viewState,
        onTabSelected = viewModel::onTabSelected,
        onCustomRangeClick = viewModel::onEditCustomRangeTapped,
        modifier = modifier
    )
}

@Composable
private fun HandleEvents(
    event: LiveData<MultiLiveEvent.Event>,
    openDatePicker: (Long, Long) -> Unit,
) {
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(event, navController, lifecycleOwner) {
        val observer = Observer { event: MultiLiveEvent.Event ->
            when (event) {
                is DashboardCouponsViewModel.OpenDatePicker -> {
                    openDatePicker(event.fromDate.time, event.toDate.time)
                }
            }
        }

        event.observe(lifecycleOwner, observer)

        onDispose {
            event.removeObserver(observer)
        }
    }
}

@Composable
private fun DashboardCouponsCard(
    dateRangeState: DateRangeState,
    viewState: State,
    onTabSelected: (SelectionType) -> Unit,
    onCustomRangeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    WidgetCard(
        titleResource = COUPONS.titleResource,
        menu = DashboardWidgetMenu(emptyList()),
        isError = viewState is Error,
        modifier = modifier
    ) {
        Column {
            DashboardDateRangeHeader(
                rangeSelection = dateRangeState.rangeSelection,
                dateFormatted = dateRangeState.rangeFormatted,
                onCustomRangeClick = onCustomRangeClick,
                onTabSelected = onTabSelected,
                modifier = Modifier.fillMaxWidth()
            )

            Divider()

            when (viewState) {
                is Loading -> {
                    CircularProgressIndicator()
                }

                is Loaded -> {
                    DashboardCouponsList(viewState)
                }

                is Error -> {
                    WidgetError(
                        onContactSupportClicked = { /*TODO*/ },
                        onRetryClicked = { /*TODO*/ }
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardCouponsList(
    state: Loaded,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        state.coupons.forEach {
            Text(text = it.code)
            Divider()
        }
    }
}