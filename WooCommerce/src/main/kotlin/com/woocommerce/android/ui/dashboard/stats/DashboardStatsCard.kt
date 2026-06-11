package com.woocommerce.android.ui.dashboard.stats

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.compose.component.WCModalBottomSheet
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.dashboard.DashboardDateRangeHeader
import com.woocommerce.android.ui.dashboard.DashboardFragmentDirections
import com.woocommerce.android.ui.dashboard.DashboardStatsUsageTracksEventEmitter
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.WCAnalyticsNotAvailableErrorView
import com.woocommerce.android.ui.dashboard.WidgetCard
import com.woocommerce.android.ui.dashboard.WidgetError
import com.woocommerce.android.ui.dashboard.defaultHideMenuEntry
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.commons.stats.StatsTimeRange
import org.wordpress.android.fluxc.model.settings.WCAnalyticsOrderDateType
import java.util.Date

@Composable
fun DashboardStatsCard(
    openDatePicker: (Long, Long, (Long, Long) -> Unit) -> Unit,
    parentViewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    viewModel: DashboardStatsViewModel = hiltViewModel(
        creationCallback = { factory: DashboardStatsViewModel.Factory ->
            factory.create(parentViewModel)
        }
    )
) {
    val dateRange = viewModel.dateRangeState.observeAsState().value ?: return
    val revenueStatsState by viewModel.revenueStatsState.observeAsState()
    val visitorsStatsState by viewModel.visitorStatsState.observeAsState()
    val lastUpdateState by viewModel.lastUpdateStats.observeAsState()
    val isScheduledImportEnabled by parentViewModel.isScheduledImportEnabled.observeAsState(false)
    val selectedRevenueStatsType = viewModel.selectedRevenueStatsType.observeAsState().value ?: return
    val orderDateTypeState by viewModel.orderDateTypeState.collectAsStateWithLifecycle()
    var showOrderDateTypeBottomSheet by rememberSaveable { mutableStateOf(false) }

    HandleEvents(
        event = viewModel.event,
        openDatePicker = { fromDate, toDate ->
            openDatePicker(fromDate, toDate) { from, to ->
                viewModel.onCustomRangeSelected(StatsTimeRange(Date(from), Date(to)))
            }
        }
    )

    WidgetCard(
        titleResource = DashboardWidget.Type.STATS.titleResource,
        menu = DashboardViewModel.DashboardWidgetMenu(
            items = listOf(
                DashboardWidget.Type.STATS.defaultHideMenuEntry {
                    parentViewModel.onHideWidgetClicked(DashboardWidget.Type.STATS)
                }
            )
        ),
        button = DashboardViewModel.DashboardWidgetAction(
            titleResource = R.string.analytics_section_see_all,
            action = viewModel::onViewAnalyticsClicked
        ),
        isError = revenueStatsState is DashboardStatsViewModel.RevenueStatsViewState.WCAnalyticsInactive ||
            revenueStatsState == DashboardStatsViewModel.RevenueStatsViewState.GenericError,
        modifier = modifier.testTag(DashboardStatsTestTags.DASHBOARD_STATS_CARD)
    ) {
        when (revenueStatsState) {
            is DashboardStatsViewModel.RevenueStatsViewState.GenericError -> {
                WidgetError(
                    onContactSupportClicked = parentViewModel::onContactSupportClicked,
                    onRetryClicked = viewModel::onRefresh
                )
            }

            !is DashboardStatsViewModel.RevenueStatsViewState.WCAnalyticsInactive -> {
                DashboardStatsContent(
                    dateRange = dateRange,
                    revenueStatsState = revenueStatsState,
                    visitorsStatsState = visitorsStatsState,
                    lastUpdateState = lastUpdateState,
                    showDelayedFooter = isScheduledImportEnabled,
                    onDelayedStatsInfoClick = parentViewModel::onDelayedStatsInfoClicked,
                    selectedRevenueStatsType = selectedRevenueStatsType,
                    selectedOrderDateType = orderDateTypeState.selectedType,
                    dateUtils = viewModel.dateUtils,
                    currencyFormatter = viewModel.currencyFormatter,
                    usageTracksEventEmitter = viewModel.usageTracksEventEmitter,
                    onAddCustomRangeClick = viewModel::onEditCustomRangeTapped,
                    onTabSelected = viewModel::onRangeChanged,
                    onRevenueStatsTypeSelected = viewModel::onRevenueStatsTypeSelected,
                    onOrderDateTypeClick = {
                        viewModel.onOrderDateTypeSelectorTapped()
                        showOrderDateTypeBottomSheet = true
                    },
                    onChartDateSelected = viewModel::onChartDateSelected
                )
            }

            else -> {
                WCAnalyticsNotAvailableErrorView(
                    title = stringResource(id = R.string.my_store_stats_plugin_inactive_title),
                    onContactSupportClick = parentViewModel::onContactSupportClicked
                )
            }
        }
    }

    if (showOrderDateTypeBottomSheet) {
        OrderDateTypeBottomSheet(
            state = orderDateTypeState,
            onDismiss = { showOrderDateTypeBottomSheet = false },
            onSelect = { orderDateType ->
                viewModel.onOrderDateTypeSelected(orderDateType) {
                    showOrderDateTypeBottomSheet = false
                }
            }
        )
    }
}

@Composable
private fun DashboardStatsContent(
    dateRange: DashboardStatsViewModel.DateRangeState,
    revenueStatsState: DashboardStatsViewModel.RevenueStatsViewState?,
    visitorsStatsState: DashboardStatsViewModel.VisitorStatsViewState?,
    lastUpdateState: Long?,
    showDelayedFooter: Boolean,
    onDelayedStatsInfoClick: () -> Unit,
    selectedRevenueStatsType: DashboardStatsViewModel.RevenueStatsType,
    selectedOrderDateType: WCAnalyticsOrderDateType,
    dateUtils: DateUtils,
    currencyFormatter: CurrencyFormatter,
    usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter,
    onAddCustomRangeClick: () -> Unit,
    onTabSelected: (SelectionType) -> Unit,
    onRevenueStatsTypeSelected: (DashboardStatsViewModel.RevenueStatsType) -> Unit,
    onOrderDateTypeClick: () -> Unit,
    onChartDateSelected: (String?) -> Unit,
) {
    Column {
        DashboardDateRangeHeader(
            rangeSelection = dateRange.rangeSelection,
            dateFormatted = dateRange.selectedDateFormatted ?: dateRange.rangeFormatted,
            onCustomRangeClick = onAddCustomRangeClick,
            onTabSelected = onTabSelected,
            modifier = Modifier.fillMaxWidth()
        )

        Divider()

        RevenueStatsTypeSelector(
            selectedType = selectedRevenueStatsType,
            onTypeSelected = onRevenueStatsTypeSelected,
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
        )

        StatsChart(
            dateRange = dateRange,
            revenueStatsState = revenueStatsState,
            visitorsStatsState = visitorsStatsState,
            lastUpdateState = lastUpdateState,
            showDelayedFooter = showDelayedFooter,
            onDelayedStatsInfoClick = onDelayedStatsInfoClick,
            selectedRevenueStatsType = selectedRevenueStatsType,
            selectedOrderDateType = selectedOrderDateType,
            dateUtils = dateUtils,
            currencyFormatter = currencyFormatter,
            usageTracksEventEmitter = usageTracksEventEmitter,
            onAddCustomRangeClick = onAddCustomRangeClick,
            onOrderDateTypeClick = onOrderDateTypeClick,
            onChartDateSelected = onChartDateSelected,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StatsChart(
    dateRange: DashboardStatsViewModel.DateRangeState,
    revenueStatsState: DashboardStatsViewModel.RevenueStatsViewState?,
    visitorsStatsState: DashboardStatsViewModel.VisitorStatsViewState?,
    lastUpdateState: Long?,
    showDelayedFooter: Boolean,
    onDelayedStatsInfoClick: () -> Unit,
    selectedRevenueStatsType: DashboardStatsViewModel.RevenueStatsType,
    selectedOrderDateType: WCAnalyticsOrderDateType,
    dateUtils: DateUtils,
    currencyFormatter: CurrencyFormatter,
    usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter,
    onAddCustomRangeClick: () -> Unit,
    onOrderDateTypeClick: () -> Unit,
    onChartDateSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope
    val context = LocalContext.current

    val statsView = remember(context) {
        DashboardStatsView(context).apply {
            initView(
                dateUtils = dateUtils,
                currencyFormatter = currencyFormatter,
                usageTracksEventEmitter = usageTracksEventEmitter,
                lifecycleScope = lifecycleScope,
                onViewAnalyticsClick = {},
                onDateSelected = onChartDateSelected
            )
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
        factory = {
            statsView.apply {
                customRangeButton.setOnClickListener { onAddCustomRangeClick() }
                setOnOrderDateTypeClickListener(onOrderDateTypeClick)
            }
        }
    )

    // Update the view using side effects
    // This is better than using [AndroidView]'s update because it allows for granular updates, while the former
    // is applying all properties on each composition (even the unchanged ones) which creates issues with the legacy
    // view.

    LaunchedEffect(dateRange.rangeSelection) {
        statsView.loadDashboardStats(dateRange.rangeSelection)
    }

    LaunchedEffect(lastUpdateState, showDelayedFooter) {
        statsView.showStatsFooter(
            lastUpdateMillis = lastUpdateState,
            isDelayed = showDelayedFooter,
            onInfoClick = onDelayedStatsInfoClick
        )
    }

    LaunchedEffect(selectedOrderDateType) {
        statsView.setOrderDateType(selectedOrderDateType)
    }

    LaunchedEffect(revenueStatsState, selectedRevenueStatsType) {
        when (revenueStatsState) {
            is DashboardStatsViewModel.RevenueStatsViewState.Content -> {
                statsView.showErrorView(false)
                statsView.showSkeleton(false)
                statsView.updateView(revenueStatsState.revenueStats, selectedRevenueStatsType)
            }

            DashboardStatsViewModel.RevenueStatsViewState.GenericError -> {
                statsView.showErrorView(true)
                statsView.showSkeleton(false)
            }

            is DashboardStatsViewModel.RevenueStatsViewState.Loading -> {
                statsView.showErrorView(false)
                statsView.showSkeleton(true)
                if (revenueStatsState.isForced) {
                    statsView.clearStatsHeaderValues()
                    statsView.clearChartData()
                }
            }

            else -> Unit
        }
    }

    LaunchedEffect(visitorsStatsState) {
        visitorsStatsState?.let {
            statsView.showVisitorStats(it)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RevenueStatsTypeSelector(
    selectedType: DashboardStatsViewModel.RevenueStatsType,
    onTypeSelected: (DashboardStatsViewModel.RevenueStatsType) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = DashboardStatsViewModel.RevenueStatsType.OPTIONS
    val segmentedButtonColors = SegmentedButtonDefaults.colors(
        activeContainerColor = colorResource(id = R.color.color_primary),
        activeContentColor = colorResource(id = R.color.woo_white),
    )

    SingleChoiceSegmentedButtonRow(
        modifier = modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp)
    ) {
        options.forEachIndexed { index, type ->
            SegmentedButton(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = segmentedButtonColors,
                icon = {},
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(id = type.labelRes),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderDateTypeBottomSheet(
    state: DashboardStatsViewModel.OrderDateTypeUiState,
    onDismiss: () -> Unit,
    onSelect: (WCAnalyticsOrderDateType) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    WCModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = stringResource(id = R.string.dashboard_stats_order_date_type_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = stringResource(id = R.string.dashboard_stats_order_date_type_sheet_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(28.dp))

            orderDateTypeOptions.forEach { option ->
                OrderDateTypeOptionRow(
                    option = option,
                    state = state,
                    onDismiss = onDismiss,
                    onSelect = onSelect
                )
            }

            if (state.hasUpdateError) {
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    text = stringResource(id = R.string.dashboard_stats_order_date_type_update_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = stringResource(id = R.string.dashboard_stats_order_date_type_sheet_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OrderDateTypeOptionRow(
    option: OrderDateTypeOption,
    state: DashboardStatsViewModel.OrderDateTypeUiState,
    onDismiss: () -> Unit,
    onSelect: (WCAnalyticsOrderDateType) -> Unit
) {
    val isSelected = state.selectedType == option.type
    val isUpdating = state.updatingType == option.type
    val isEnabled = state.updatingType == null && !state.isLoading

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .clickable(enabled = isEnabled) {
                if (isSelected) {
                    onDismiss()
                } else {
                    onSelect(option.type)
                }
            }
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = option.title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(id = option.description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        when {
            isUpdating -> CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )

            isSelected -> Icon(
                modifier = Modifier.size(18.dp),
                imageVector = ImageVector.vectorResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private data class OrderDateTypeOption(
    val type: WCAnalyticsOrderDateType,
    @StringRes val title: Int,
    @StringRes val description: Int
)

private val orderDateTypeOptions = listOf(
    OrderDateTypeOption(
        type = WCAnalyticsOrderDateType.PAID,
        title = R.string.dashboard_stats_paid_orders,
        description = R.string.dashboard_stats_paid_orders_description
    ),
    OrderDateTypeOption(
        type = WCAnalyticsOrderDateType.CREATED,
        title = R.string.dashboard_stats_placed_orders,
        description = R.string.dashboard_stats_placed_orders_description
    ),
    OrderDateTypeOption(
        type = WCAnalyticsOrderDateType.COMPLETED,
        title = R.string.dashboard_stats_completed_orders,
        description = R.string.dashboard_stats_completed_orders_description
    )
)

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
                is DashboardStatsViewModel.OpenDatePicker -> {
                    openDatePicker(event.fromDate.time, event.toDate.time)
                }

                is DashboardStatsViewModel.OpenAnalytics -> {
                    navController.navigateSafely(
                        DashboardFragmentDirections.actionDashboardToAnalytics(event.analyticsPeriod)
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
