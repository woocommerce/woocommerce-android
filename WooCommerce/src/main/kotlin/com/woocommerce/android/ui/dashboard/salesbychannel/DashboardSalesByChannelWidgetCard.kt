package com.woocommerce.android.ui.dashboard.salesbychannel

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.dashboard.DashboardDateRangeHeader
import com.woocommerce.android.ui.dashboard.DashboardFragmentDirections
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.OpenRangePicker
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetAction
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.WCAnalyticsNotAvailableErrorView
import com.woocommerce.android.ui.dashboard.WidgetCard
import com.woocommerce.android.ui.dashboard.WidgetError
import com.woocommerce.android.ui.dashboard.salesbychannel.DashboardSalesByChannelViewModel.ChannelSalesUiModel
import com.woocommerce.android.ui.dashboard.salesbychannel.DashboardSalesByChannelViewModel.OpenAnalytics
import com.woocommerce.android.ui.dashboard.salesbychannel.DashboardSalesByChannelViewModel.OpenDatePicker
import com.woocommerce.android.ui.dashboard.salesbychannel.DashboardSalesByChannelViewModel.SalesByChannelDateRange
import com.woocommerce.android.ui.dashboard.salesbychannel.DashboardSalesByChannelViewModel.SalesByChannelState
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.commons.stats.StatsTimeRange
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val DASHBOARD_SALES_BY_CHANNEL_CARD = "dashboard_sales_by_channel_card"

@Composable
fun DashboardSalesByChannelWidgetCard(
    parentViewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    salesByChannelViewModel: DashboardSalesByChannelViewModel = hiltViewModel(
        creationCallback = { factory: DashboardSalesByChannelViewModel.Factory ->
            factory.create(parentViewModel)
        }
    )
) {
    salesByChannelViewModel.salesByChannelState.observeAsState().value?.let { state ->
        val lastUpdateState by salesByChannelViewModel.lastUpdate.observeAsState()
        val selectedDateRange by salesByChannelViewModel.selectedDateRange.observeAsState()
        WidgetCard(
            titleResource = state.titleStringRes,
            menu = state.menu,
            button = state.onOpenAnalyticsTapped,
            modifier = modifier.testTag(DASHBOARD_SALES_BY_CHANNEL_CARD),
            isError = state.error != null
        ) {
            when {
                state.error != null -> SalesByChannelErrorView(
                    errorType = state.error,
                    onContactSupportClicked = parentViewModel::onContactSupportClicked,
                    onRetryClicked = salesByChannelViewModel::onRefresh
                )

                else -> DashboardSalesByChannelContent(
                    state = state,
                    selectedDateRange = selectedDateRange,
                    lastUpdateState = lastUpdateState,
                    onTabSelected = salesByChannelViewModel::onRangeChanged,
                    onEditCustomRangeTapped = salesByChannelViewModel::onEditCustomRangeTapped
                )
            }
        }
    }

    val openDatePicker = { start: Long, end: Long, callback: (Long, Long) -> Unit ->
        parentViewModel.onDashboardWidgetEvent(
            OpenRangePicker(start, end, callback)
        )
    }
    HandleEvents(
        salesByChannelViewModel.event,
        openDatePicker = { fromDate, toDate ->
            openDatePicker(fromDate, toDate) { from, to ->
                salesByChannelViewModel.onCustomRangeSelected(
                    StatsTimeRange(Date(from), Date(to))
                )
            }
        }
    )
}

@Composable
fun DashboardSalesByChannelContent(
    state: SalesByChannelState?,
    selectedDateRange: SalesByChannelDateRange?,
    lastUpdateState: String?,
    onTabSelected: (SelectionType) -> Unit,
    onEditCustomRangeTapped: () -> Unit,
) {
    Column {
        selectedDateRange?.let {
            DashboardDateRangeHeader(
                rangeSelection = it.rangeSelection,
                dateFormatted = it.dateFormatted,
                onCustomRangeClick = onEditCustomRangeTapped,
                onTabSelected = onTabSelected
            )
        }
        Divider(modifier = Modifier.padding(bottom = 16.dp))

        when {
            state?.isLoading == true -> SalesByChannelLoading(
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            else -> SalesByChannelContentBody(
                state = state,
                lastUpdateState = lastUpdateState
            )
        }
    }
}

@Composable
private fun HandleEvents(
    event: LiveData<Event>,
    openDatePicker: (Long, Long) -> Unit,
) {
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(event, navController, lifecycleOwner) {
        val observer = Observer { event: Event ->
            when (event) {
                is OpenDatePicker -> openDatePicker(
                    event.fromDate.time,
                    event.toDate.time
                )

                is OpenAnalytics -> {
                    navController.navigateSafely(
                        DashboardFragmentDirections
                            .actionDashboardToAnalytics(event.analyticsPeriod)
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
private fun SalesByChannelContentBody(
    state: SalesByChannelState?,
    lastUpdateState: String?,
) {
    Column {
        when {
            state?.channels.isNullOrEmpty() -> SalesByChannelEmptyView()
            else -> SalesByChannelList(
                channels = state.channels,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (!lastUpdateState.isNullOrEmpty()) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally),
                text = lastUpdateState,
                style = MaterialTheme.typography.body2,
                color = colorResource(id = R.color.color_on_surface_medium),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SalesByChannelList(
    channels: List<ChannelSalesUiModel>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        channels.forEachIndexed { index, channel ->
            ChannelBarItem(
                channel = channel,
                displayDivider = index != channels.size - 1
            )
        }
    }
}

@Composable
private fun ChannelBarItem(
    channel: ChannelSalesUiModel,
    modifier: Modifier = Modifier,
    displayDivider: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.channelName,
                    style = MaterialTheme.typography.subtitle1,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(channel.currentBarFraction.coerceAtLeast(0.01f))
                        .height(8.dp)
                        .background(
                            color = colorResource(id = R.color.woo_purple_40),
                            shape = RoundedCornerShape(4.dp)
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(channel.compareBarFraction.coerceAtLeast(0.01f))
                        .height(8.dp)
                        .background(
                            color = colorResource(id = R.color.woo_purple_5),
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = channel.revenueFormatted,
                    style = MaterialTheme.typography.subtitle1,
                    textAlign = TextAlign.End
                )
                Text(
                    text = channel.percentageChange,
                    style = MaterialTheme.typography.body2,
                    color = if (channel.isPositiveChange) {
                        colorResource(id = R.color.woo_green_50)
                    } else {
                        colorResource(id = R.color.color_error)
                    },
                    textAlign = TextAlign.End
                )
            }
        }
        if (displayDivider) {
            Divider(modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun SalesByChannelLoading(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(5) {
            SalesByChannelSkeletonItem()
            Divider()
        }
    }
}

@Composable
private fun SalesByChannelSkeletonItem() {
    Row(
        modifier = Modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SkeletonView(
                modifier = Modifier
                    .height(14.dp)
                    .fillMaxWidth(0.5f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonView(
                modifier = Modifier
                    .height(8.dp)
                    .fillMaxWidth(0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonView(
                modifier = Modifier
                    .height(8.dp)
                    .fillMaxWidth(0.4f)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(horizontalAlignment = Alignment.End) {
            SkeletonView(
                modifier = Modifier
                    .height(14.dp)
                    .width(60.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonView(
                modifier = Modifier
                    .height(12.dp)
                    .width(40.dp)
            )
        }
    }
}

@Composable
private fun SalesByChannelEmptyView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_not_found),
            contentDescription = "",
        )
        Text(
            modifier = Modifier.padding(top = 24.dp),
            text = stringResource(id = R.string.dashboard_sales_by_channel_empty),
            style = MaterialTheme.typography.body2,
        )
    }
}

@Composable
private fun SalesByChannelErrorView(
    errorType: DashboardSalesByChannelViewModel.ErrorType,
    onContactSupportClicked: () -> Unit,
    onRetryClicked: () -> Unit
) {
    when (errorType) {
        DashboardSalesByChannelViewModel.ErrorType.WCAnalyticsInactive -> {
            WCAnalyticsNotAvailableErrorView(
                title = stringResource(
                    id = R.string.dashboard_sales_by_channel_analytics_inactive_title
                ),
                onContactSupportClick = onContactSupportClicked
            )
        }

        else -> {
            WidgetError(
                onContactSupportClicked = onContactSupportClicked,
                onRetryClicked = onRetryClicked
            )
        }
    }
}

@LightDarkThemePreviews
@Composable
private fun SalesByChannelWidgetCardPreview() {
    val selectedDateRange = SalesByChannelDateRange(
        SelectionType.TODAY.generateSelectionData(
            referenceStartDate = Date(),
            referenceEndDate = Date(),
            calendar = Calendar.getInstance(),
            locale = Locale.getDefault(),
        ),
        customRange = null,
        dateFormatted = "Today"
    )
    val state = SalesByChannelState(
        channels = listOf(
            ChannelSalesUiModel(
                channelName = "Web",
                revenueFormatted = "$1,200",
                percentageChange = "+12.5%",
                currentBarFraction = 1.0f,
                compareBarFraction = 0.7f,
                isPositiveChange = true
            ),
            ChannelSalesUiModel(
                channelName = "Mobile app",
                revenueFormatted = "$800",
                percentageChange = "-5.2%",
                currentBarFraction = 0.67f,
                compareBarFraction = 0.85f,
                isPositiveChange = false
            ),
            ChannelSalesUiModel(
                channelName = "Social",
                revenueFormatted = "$350",
                percentageChange = "+45.0%",
                currentBarFraction = 0.29f,
                compareBarFraction = 0.15f,
                isPositiveChange = true
            ),
        ),
        isLoading = false,
        titleStringRes = R.string.my_store_widget_sales_by_channel_title,
        menu = DashboardWidgetMenu(emptyList()),
        onOpenAnalyticsTapped = DashboardWidgetAction(
            titleResource = R.string.analytics_section_see_all,
            action = {}
        )
    )
    Column {
        DashboardSalesByChannelContent(
            state = state,
            lastUpdateState = "Last update: 8:52 AM",
            selectedDateRange = selectedDateRange,
            onTabSelected = {},
            onEditCustomRangeTapped = {}
        )
        DashboardSalesByChannelContent(
            state = state.copy(isLoading = true),
            lastUpdateState = "Last update: 8:52 AM",
            selectedDateRange = selectedDateRange,
            onTabSelected = {},
            onEditCustomRangeTapped = {}
        )
        DashboardSalesByChannelContent(
            state = state.copy(
                error = DashboardSalesByChannelViewModel.ErrorType.Generic
            ),
            lastUpdateState = "Last update: 8:52 AM",
            selectedDateRange = selectedDateRange,
            onTabSelected = {},
            onEditCustomRangeTapped = {}
        )
        DashboardSalesByChannelContent(
            state = state.copy(channels = emptyList()),
            lastUpdateState = "Last update: 8:52 AM",
            selectedDateRange = selectedDateRange,
            onTabSelected = {},
            onEditCustomRangeTapped = {}
        )
    }
}
