package com.woocommerce.android.ui.dashboard.topperformers

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.compose.component.ProductThumbnail
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooDivider
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.dashboard.DashboardDateRangeHeader
import com.woocommerce.android.ui.dashboard.DashboardFragmentDirections
import com.woocommerce.android.ui.dashboard.DashboardSkeleton
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.OpenRangePicker
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetAction
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.StatsInfoFooter
import com.woocommerce.android.ui.dashboard.TopPerformerProductUiModel
import com.woocommerce.android.ui.dashboard.WCAnalyticsNotAvailableErrorView
import com.woocommerce.android.ui.dashboard.WidgetCard
import com.woocommerce.android.ui.dashboard.WidgetError
import com.woocommerce.android.ui.dashboard.stats.DashboardStatsTestTags
import com.woocommerce.android.ui.dashboard.topperformers.DashboardTopPerformersViewModel.OpenAnalytics
import com.woocommerce.android.ui.dashboard.topperformers.DashboardTopPerformersViewModel.OpenDatePicker
import com.woocommerce.android.ui.dashboard.topperformers.DashboardTopPerformersViewModel.OpenTopPerformer
import com.woocommerce.android.ui.dashboard.topperformers.DashboardTopPerformersViewModel.TopPerformersDateRange
import com.woocommerce.android.ui.dashboard.topperformers.DashboardTopPerformersViewModel.TopPerformersState
import com.woocommerce.android.ui.products.details.ProductDetailFragment.Mode.ShowProduct
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.commons.stats.StatsTimeRange
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardTopPerformersWidgetCard(
    parentViewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    topPerformersViewModel: DashboardTopPerformersViewModel = hiltViewModel(
        creationCallback = { factory: DashboardTopPerformersViewModel.Factory ->
            factory.create(parentViewModel)
        }
    )
) {
    topPerformersViewModel.topPerformersState.observeAsState().value?.let { topPerformersState ->
        val lastUpdateState by topPerformersViewModel.lastUpdateTopPerformers.observeAsState()
        val isScheduledImportEnabled by parentViewModel.isScheduledImportEnabled.observeAsState(false)
        val selectedDateRange by topPerformersViewModel.selectedDateRange.observeAsState()
        WidgetCard(
            titleResource = topPerformersState.titleStringRes,
            menu = topPerformersState.menu,
            button = topPerformersState.onOpenAnalyticsTapped,
            modifier = modifier.testTag(DashboardStatsTestTags.DASHBOARD_TOP_PERFORMERS_CARD),
            isError = topPerformersState.error != null
        ) {
            when {
                topPerformersState.error != null -> TopPerformersErrorView(
                    errorType = topPerformersState.error,
                    onContactSupportClicked = parentViewModel::onContactSupportClicked,
                    onRetryClicked = topPerformersViewModel::onRefresh
                )

                else -> DashboardTopPerformersContent(
                    topPerformersState = topPerformersState,
                    selectedDateRange = selectedDateRange,
                    lastUpdateState = lastUpdateState,
                    showDelayedFooter = isScheduledImportEnabled,
                    onDelayedStatsInfoClick = parentViewModel::onDelayedStatsInfoClicked,
                    onTabSelected = topPerformersViewModel::onRangeChanged,
                    onEditCustomRangeTapped = topPerformersViewModel::onEditCustomRangeTapped
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
        topPerformersViewModel.event,
        openDatePicker = { fromDate, toDate ->
            openDatePicker(fromDate, toDate) { from, to ->
                topPerformersViewModel.onCustomRangeSelected(StatsTimeRange(Date(from), Date(to)))
            }
        }
    )
}

@Composable
fun DashboardTopPerformersContent(
    topPerformersState: TopPerformersState?,
    selectedDateRange: TopPerformersDateRange?,
    lastUpdateState: String?,
    showDelayedFooter: Boolean,
    onDelayedStatsInfoClick: () -> Unit,
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
        WooDivider(modifier = Modifier.padding(bottom = 16.dp))

        when {
            topPerformersState?.isLoading == true -> TopPerformersLoading(
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            else -> {
                TopPerformersContent(
                    topPerformersState = topPerformersState,
                    lastUpdateState = lastUpdateState,
                    showDelayedFooter = showDelayedFooter,
                    onDelayedStatsInfoClick = onDelayedStatsInfoClick
                )
            }
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
                is OpenTopPerformer -> navController.navigateSafely(
                    NavGraphMainDirections.actionGlobalProductDetailFragment(
                        mode = ShowProduct(event.productId),
                    )
                )

                is OpenDatePicker -> openDatePicker(event.fromDate.time, event.toDate.time)
                is OpenAnalytics -> {
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

@Composable
private fun TopPerformersContent(
    topPerformersState: TopPerformersState?,
    lastUpdateState: String?,
    showDelayedFooter: Boolean,
    onDelayedStatsInfoClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.product),
                style = WooTheme.text.bodyMedium.emphasized,
                color = WooTheme.colors.surface.onDefault,
            )
            Text(
                text = stringResource(id = R.string.dashboard_top_performers_items_sold),
                style = WooTheme.text.bodyMedium.emphasized,
                color = WooTheme.colors.surface.onDefault,
            )
        }
        when {
            topPerformersState?.topPerformers.isNullOrEmpty() -> TopPerformersEmptyView()
            else -> TopPerformerProductList(
                topPerformers = topPerformersState.topPerformers,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        val footerText = if (showDelayedFooter) {
            stringResource(id = R.string.dashboard_stats_delayed_footer)
        } else {
            lastUpdateState
        }
        if (!footerText.isNullOrEmpty()) {
            StatsInfoFooter(text = footerText, onInfoClick = onDelayedStatsInfoClick)
        }
    }
}

@Composable
private fun TopPerformerProductList(
    topPerformers: List<TopPerformerProductUiModel>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        topPerformers.forEachIndexed { index, product ->
            TopPerformerProductItem(
                topPerformer = product,
                onItemClicked = product.onClick,
                displayDivider = index != topPerformers.size - 1
            )
        }
    }
}

@Composable
private fun TopPerformersLoading(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(5) {
            TopPerformerSkeletonItem()
            WooDivider()
        }
    }
}

@Composable
fun TopPerformerSkeletonItem() {
    Row(
        modifier = Modifier
            .padding(top = 16.dp, bottom = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DashboardSkeleton(
            modifier = Modifier
                .height(42.dp)
                .width(42.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            DashboardSkeleton(
                modifier = Modifier
                    .height(14.dp)
                    .width(180.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            DashboardSkeleton(
                modifier = Modifier
                    .height(12.dp)
                    .width(150.dp)
            )
        }
        DashboardSkeleton(
            modifier = Modifier
                .height(14.dp)
                .width(30.dp)
        )
    }
}

@Composable
private fun TopPerformerProductItem(
    topPerformer: TopPerformerProductUiModel,
    onItemClicked: (Long) -> Unit,
    modifier: Modifier = Modifier,
    displayDivider: Boolean
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onItemClicked(topPerformer.productId) }
            .padding(start = 16.dp, end = 16.dp, top = 8.dp),
    ) {
        ProductThumbnail(
            imageUrl = topPerformer.imageUrl ?: "",
            contentDescription = stringResource(id = R.string.product_image_content_description),
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Row(modifier = Modifier.padding(bottom = 4.dp, end = 16.dp)) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = topPerformer.name,
                    style = WooTheme.text.titleMedium.regular,
                    color = WooTheme.colors.surface.onDefault,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = topPerformer.timesOrdered,
                    style = WooTheme.text.titleMedium.regular,
                    color = WooTheme.colors.surface.onDefault,
                )
            }
            Text(
                text = topPerformer.netSales,
                style = WooTheme.text.bodyMedium.regular,
                color = WooTheme.colors.surface.onDefault,
            )
            if (displayDivider) {
                WooDivider(modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun TopPerformersEmptyView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_not_found),
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(top = 24.dp),
            text = stringResource(id = R.string.dashboard_top_performers_empty),
            style = WooTheme.text.bodyMedium.regular,
            color = WooTheme.colors.surface.onDefault,
        )
    }
}

@Composable
private fun TopPerformersErrorView(
    errorType: DashboardTopPerformersViewModel.ErrorType,
    onContactSupportClicked: () -> Unit,
    onRetryClicked: () -> Unit
) {
    when (errorType) {
        DashboardTopPerformersViewModel.ErrorType.WCAnalyticsInactive -> {
            WCAnalyticsNotAvailableErrorView(
                title = stringResource(id = R.string.dashboard_top_performers_wcanalytics_inactive_title),
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

@PreviewLightDark
@Composable
private fun TopPerformersWidgetCardPreview() =
    WooDesignSystemThemeWithBackground { TopPerformersPreviewContent() }

@Composable
private fun TopPerformersPreviewContent() {
    val selectedDateRange = TopPerformersDateRange(
        SelectionType.TODAY.generateSelectionData(
            referenceStartDate = Date(),
            referenceEndDate = Date(),
            calendar = Calendar.getInstance(),
            locale = Locale.getDefault(),
        ),
        customRange = null,
        dateFormatted = "Today"
    )
    val topPerformersState = TopPerformersState(
        topPerformers = listOf(
            TopPerformerProductUiModel(
                productId = 1,
                name = "Product 1",
                timesOrdered = "10 times",
                netSales = "$100",
                imageUrl = "",
                onClick = {}
            ),
            TopPerformerProductUiModel(
                productId = 2,
                name = "Product 2",
                timesOrdered = "20 times",
                netSales = "$200",
                imageUrl = "",
                onClick = {}
            ),
            TopPerformerProductUiModel(
                productId = 3,
                name = "Product 3",
                timesOrdered = "30 times",
                netSales = "$300",
                imageUrl = "",
                onClick = {}
            ),
        ),
        isLoading = false,
        titleStringRes = DashboardWidget.Type.POPULAR_PRODUCTS.titleResource,
        menu = DashboardWidgetMenu(emptyList()),
        onOpenAnalyticsTapped = DashboardWidgetAction(
            titleResource = R.string.analytics_section_see_all,
            action = {}
        )
    )
    Column {
        DashboardTopPerformersContent(
            topPerformersState = topPerformersState,
            lastUpdateState = "Last update: 8:52 AM",
            selectedDateRange = selectedDateRange,
            showDelayedFooter = false,
            onDelayedStatsInfoClick = {},
            onTabSelected = {},
            onEditCustomRangeTapped = {}
        )
        DashboardTopPerformersContent(
            topPerformersState = topPerformersState.copy(isLoading = true),
            lastUpdateState = "Last update: 8:52 AM",
            selectedDateRange = selectedDateRange,
            showDelayedFooter = false,
            onDelayedStatsInfoClick = {},
            onTabSelected = {},
            onEditCustomRangeTapped = {}
        )
        DashboardTopPerformersContent(
            topPerformersState = topPerformersState.copy(
                error = DashboardTopPerformersViewModel.ErrorType.Generic
            ),
            lastUpdateState = "Last update: 8:52 AM",
            selectedDateRange = selectedDateRange,
            showDelayedFooter = false,
            onDelayedStatsInfoClick = {},
            onTabSelected = {},
            onEditCustomRangeTapped = {}
        )
        DashboardTopPerformersContent(
            topPerformersState = topPerformersState.copy(topPerformers = emptyList()),
            lastUpdateState = "Last update: 8:52 AM",
            selectedDateRange = selectedDateRange,
            showDelayedFooter = true,
            onDelayedStatsInfoClick = {},
            onTabSelected = {},
            onEditCustomRangeTapped = {}
        )
    }
}
