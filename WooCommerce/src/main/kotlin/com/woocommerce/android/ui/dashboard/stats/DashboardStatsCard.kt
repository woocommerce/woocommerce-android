package com.woocommerce.android.ui.dashboard.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRange
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.compose.viewModelWithFactory
import com.woocommerce.android.ui.dashboard.DashboardFragmentDirections
import com.woocommerce.android.ui.dashboard.DashboardStatsUsageTracksEventEmitter
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.WidgetCard
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import java.util.Date

@Composable
fun DashboardStatsCard(
    dateUtils: DateUtils,
    currencyFormatter: CurrencyFormatter,
    usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter,
    onPluginUnavailableError: () -> Unit,
    onStatsError: () -> Unit,
    openDatePicker: (Long, Long, (Long, Long) -> Unit) -> Unit,
    parentViewModel: DashboardViewModel,
    viewModel: DashboardStatsViewModel = viewModelWithFactory<DashboardStatsViewModel, DashboardStatsViewModel.Factory>(
        creationCallback = {
            it.create(parentViewModel)
        }
    )
) {
    val dateRange by viewModel.dateRangeState.observeAsState()
    val revenueStatsState by viewModel.revenueStatsState.observeAsState()
    val visitorsStatsState by viewModel.visitorStatsState.observeAsState()
    val lastUpdateState by viewModel.lastUpdateStats.observeAsState()

    LaunchedEffect(revenueStatsState) {
        when (revenueStatsState) {
            is DashboardStatsViewModel.RevenueStatsViewState.GenericError -> onStatsError()
            is DashboardStatsViewModel.RevenueStatsViewState.PluginNotActiveError -> onPluginUnavailableError()
            else -> Unit
        }
    }

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
                DashboardViewModel.DashboardWidgetAction(
                    titleResource = R.string.dynamic_dashboard_hide_widget_menu_item,
                    action = { /* TODO */ }
                )
            )
        )
    ) {
        DashboardStatsContent(
            dateRange = dateRange,
            revenueStatsState = revenueStatsState,
            visitorsStatsState = visitorsStatsState,
            lastUpdateState = lastUpdateState,
            dateUtils = dateUtils,
            currencyFormatter = currencyFormatter,
            usageTracksEventEmitter = usageTracksEventEmitter,
            onViewAnalyticsClick = viewModel::onViewAnalyticsClicked,
            onAddCustomRangeClick = viewModel::onAddCustomRangeClicked,
            onTabSelected = viewModel::onTabSelected,
            onChartDateSelected = viewModel::onChartDateSelected
        )
    }
}

@Composable
private fun DashboardStatsContent(
    dateRange: DashboardStatsViewModel.DateRangeState?,
    revenueStatsState: DashboardStatsViewModel.RevenueStatsViewState?,
    visitorsStatsState: DashboardStatsViewModel.VisitorStatsViewState?,
    lastUpdateState: Long?,
    dateUtils: DateUtils,
    currencyFormatter: CurrencyFormatter,
    usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter,
    onViewAnalyticsClick: () -> Unit,
    onAddCustomRangeClick: () -> Unit,
    onTabSelected: (SelectionType) -> Unit,
    onChartDateSelected: (String?) -> Unit
) {
    Column {
        StatsHeader(
            dateRange = dateRange,
            onCustomRangeClick = onAddCustomRangeClick,
            onTabSelected = onTabSelected,
            modifier = Modifier.fillMaxWidth()
        )
        Divider()
        StatsChart(
            dateRange = dateRange,
            revenueStatsState = revenueStatsState,
            visitorsStatsState = visitorsStatsState,
            lastUpdateState = lastUpdateState,
            dateUtils = dateUtils,
            currencyFormatter = currencyFormatter,
            usageTracksEventEmitter = usageTracksEventEmitter,
            onAddCustomRangeClick = onAddCustomRangeClick,
            onChartDateSelected = onChartDateSelected,
            modifier = Modifier.fillMaxWidth()
        )

        WCTextButton(
            onClick = onViewAnalyticsClick,
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
        ) {
            Text(
                text = stringResource(id = R.string.analytics_section_see_all),
                style = MaterialTheme.typography.body1
            )
        }
    }
}

@Composable
private fun StatsHeader(
    dateRange: DashboardStatsViewModel.DateRangeState?,
    onCustomRangeClick: () -> Unit,
    onTabSelected: (SelectionType) -> Unit,
    modifier: Modifier = Modifier
) {
    if (dateRange == null) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100)),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(
            start = dimensionResource(id = R.dimen.major_100)
        )
    ) {
        Text(
            text = stringResource(
                id = when (dateRange.rangeSelection.selectionType) {
                    SelectionType.TODAY -> R.string.today
                    SelectionType.WEEK_TO_DATE -> R.string.this_week
                    SelectionType.MONTH_TO_DATE -> R.string.this_month
                    SelectionType.YEAR_TO_DATE -> R.string.this_year
                    SelectionType.CUSTOM -> R.string.date_timeframe_custom
                    else -> error("Invalid selection type")
                }
            ),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface
        )
        val isCustomRange = dateRange.rangeSelection.selectionType == SelectionType.CUSTOM

        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100)),
            modifier = Modifier
                .then(if (isCustomRange) Modifier.clickable(onClick = onCustomRangeClick) else Modifier)
                .padding(dimensionResource(id = R.dimen.minor_100))
        ) {
            Text(
                text = dateRange.selectedDateFormatted ?: dateRange.rangeFormatted,
                style = MaterialTheme.typography.body2,
                color = if (isCustomRange) {
                    MaterialTheme.colors.primary
                } else {
                    MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
                }
            )
            if (isCustomRange) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(dimensionResource(id = R.dimen.image_minor_40))
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Box {
            var isMenuExpanded by remember { mutableStateOf(false) }
            IconButton(onClick = { isMenuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
                )
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false }
            ) {
                DashboardViewModel.SUPPORTED_RANGES_ON_MY_STORE_TAB.forEach {
                    DropdownMenuItem(
                        onClick = {
                            onTabSelected(it)
                            isMenuExpanded = false
                        }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.minor_100))
                        ) {
                            Text(text = it.title)
                            Spacer(modifier = Modifier.weight(1f))
                            if (dateRange.rangeSelection.selectionType == it) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(id = androidx.compose.ui.R.string.selected),
                                    tint = MaterialTheme.colors.primary
                                )
                            } else {
                                Spacer(modifier = Modifier.size(dimensionResource(R.dimen.image_minor_50)))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsChart(
    dateRange: DashboardStatsViewModel.DateRangeState?,
    revenueStatsState: DashboardStatsViewModel.RevenueStatsViewState?,
    visitorsStatsState: DashboardStatsViewModel.VisitorStatsViewState?,
    lastUpdateState: Long?,
    dateUtils: DateUtils,
    currencyFormatter: CurrencyFormatter,
    usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter,
    onAddCustomRangeClick: () -> Unit,
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
        modifier = modifier,
        factory = {
            statsView.apply {
                customRangeButton.setOnClickListener { onAddCustomRangeClick() }
                customRangeLabel.setOnClickListener { onAddCustomRangeClick() }
            }
        }
    )

    // Update the view using side effects
    // This is better than using [AndroidView]'s update because it allows for granular updates, while the former
    // is applying all properties on each composition (even the unchanged ones) which creates issues with the legacy
    // view.

    LaunchedEffect(dateRange?.rangeSelection) {
        dateRange?.rangeSelection?.let { statsView.loadDashboardStats(it) }
    }

    LaunchedEffect(lastUpdateState) {
        statsView.showLastUpdate(lastUpdateState)
    }

    LaunchedEffect(revenueStatsState) {
        when (revenueStatsState) {
            is DashboardStatsViewModel.RevenueStatsViewState.Content -> {
                statsView.showErrorView(false)
                statsView.showSkeleton(false)
                statsView.updateView(revenueStatsState.revenueStats)
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

private val SelectionType.title: String
    @Composable
    get() = when (this) {
        SelectionType.TODAY -> stringResource(id = R.string.today)
        SelectionType.WEEK_TO_DATE -> stringResource(id = R.string.this_week)
        SelectionType.MONTH_TO_DATE -> stringResource(id = R.string.this_month)
        SelectionType.YEAR_TO_DATE -> stringResource(id = R.string.this_year)
        SelectionType.CUSTOM -> stringResource(id = R.string.date_timeframe_custom)
        else -> error("Invalid selection type")
    }
