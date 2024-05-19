package com.woocommerce.android.ui.dashboard.coupons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.navOptions
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.model.DashboardWidget.Type.COUPONS
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRange
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.compose.viewModelWithFactory
import com.woocommerce.android.ui.coupons.CouponListFragmentDirections
import com.woocommerce.android.ui.dashboard.DashboardDateRangeHeader
import com.woocommerce.android.ui.dashboard.DashboardFragmentDirections
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.WidgetCard
import com.woocommerce.android.ui.dashboard.WidgetError
import com.woocommerce.android.ui.dashboard.coupons.DashboardCouponsViewModel.DateRangeState
import com.woocommerce.android.ui.dashboard.coupons.DashboardCouponsViewModel.State
import com.woocommerce.android.ui.dashboard.defaultHideMenuEntry
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
        onViewAllClick = viewModel::onViewAllClicked,
        onCouponClick = viewModel::onCouponClicked,
        onHideClick = { parentViewModel.onHideWidgetClicked(DashboardWidget.Type.COUPONS) },
        onRetryClick = viewModel::onRetryClicked,
        onContactSupportClick = parentViewModel::onContactSupportClicked,
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

                DashboardCouponsViewModel.ViewAllCoupons -> {
                    navController.navigateSafely(
                        DashboardFragmentDirections.actionDashboardToCouponListFragment()
                    )
                }

                is DashboardCouponsViewModel.ViewCouponDetails -> {
                    navController.navigateSafely(
                        DashboardFragmentDirections.actionDashboardToCouponListFragment()
                    )
                    navController.navigateSafely(
                        directions = CouponListFragmentDirections.actionCouponListFragmentToCouponDetailsFragment(
                            couponId = event.couponId
                        ),
                        skipThrottling = true,
                        navOptions = navOptions {
                            popUpTo(R.id.dashboard)
                            anim {
                                enter = R.anim.default_enter_anim
                                exit = R.anim.default_exit_anim
                                popEnter = R.anim.default_pop_enter_anim
                                popExit = R.anim.default_pop_exit_anim
                            }
                        }
                    )
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
    onViewAllClick: () -> Unit,
    onCouponClick: (Long) -> Unit,
    onHideClick: () -> Unit,
    onRetryClick: () -> Unit,
    onContactSupportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    WidgetCard(
        titleResource = COUPONS.titleResource,
        menu = DashboardWidgetMenu(
            listOf(
                DashboardWidget.Type.COUPONS.defaultHideMenuEntry {
                    onHideClick()
                }
            )
        ),
        button = DashboardViewModel.DashboardWidgetAction(
            titleResource = R.string.dashboard_coupons_view_all_button,
            action = onViewAllClick
        ),
        isError = viewState is Error,
        modifier = modifier
    ) {
        Column {
            if (viewState !is Error) {
                DashboardDateRangeHeader(
                    rangeSelection = dateRangeState.rangeSelection,
                    dateFormatted = dateRangeState.rangeFormatted,
                    onCustomRangeClick = onCustomRangeClick,
                    onTabSelected = onTabSelected,
                    modifier = Modifier.fillMaxWidth()
                )

                Divider()
            }

            when (viewState) {
                is State.Loading -> {
                    CouponsLoading()
                }

                is State.Loaded -> {
                    DashboardCouponsList(
                        viewState,
                        onCouponClick
                    )
                }

                is State.Error -> {
                    WidgetError(
                        onContactSupportClicked = onContactSupportClick,
                        onRetryClicked = onRetryClick
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardCouponsList(
    state: State.Loaded,
    onCouponClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(vertical = 8.dp)) {
        Header(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))
        state.coupons.forEach { couponUiModel ->
            CouponListItem(
                couponUiModel = couponUiModel,
                onClick = { onCouponClick(couponUiModel.id) }
            )
        }
    }
}

@Composable
private fun CouponListItem(
    couponUiModel: DashboardCouponsViewModel.CouponUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(top = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = couponUiModel.code,
                style = MaterialTheme.typography.subtitle1
            )
            Text(
                text = couponUiModel.uses.toString(),
                style = MaterialTheme.typography.subtitle1
            )
        }
        Text(
            text = couponUiModel.description,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier)
        Divider(modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
private fun CouponsLoading(
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(vertical = 8.dp)) {
        Header(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))
        repeat(3) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    SkeletonView(width = 260.dp, height = 16.dp)
                    SkeletonView(width = 40.dp, height = 16.dp)
                }
                SkeletonView(
                    modifier = Modifier
                        .size(width = 120.dp, height = 16.dp)
                        .padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier)
                Divider(Modifier.padding(start = 16.dp))
            }
        }
    }
}

@Composable
private fun Header(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
    ) {
        Text(
            text = stringResource(id = R.string.dashboard_coupons_card_header_coupons),
            style = MaterialTheme.typography.subtitle2,
            color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
        )
        Text(
            text = stringResource(id = R.string.dashboard_coupons_card_header_uses),
            style = MaterialTheme.typography.subtitle2,
            color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
        )
    }
}
