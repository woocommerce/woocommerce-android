package com.woocommerce.android.ui.dashboard.topcategories

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.dashboard.DashboardDateRangeHeader
import com.woocommerce.android.ui.dashboard.DashboardFragmentDirections
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.OpenRangePicker
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.TopPerformerCategoryUiModel
import com.woocommerce.android.ui.dashboard.WCAnalyticsNotAvailableErrorView
import com.woocommerce.android.ui.dashboard.WidgetCard
import com.woocommerce.android.ui.dashboard.WidgetError
import com.woocommerce.android.ui.dashboard.topcategories.DashboardTopCategoriesViewModel.OpenCategoryProducts
import com.woocommerce.android.ui.dashboard.topcategories.DashboardTopCategoriesViewModel.OpenDatePicker
import com.woocommerce.android.ui.dashboard.topcategories.DashboardTopCategoriesViewModel.TopCategoriesDateRange
import com.woocommerce.android.ui.dashboard.topcategories.DashboardTopCategoriesViewModel.TopCategoriesState
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.commons.stats.StatsTimeRange
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val DASHBOARD_TOP_CATEGORIES_CARD = "dashboard_top_categories_card"
private val ITEMS_SOLD_COLUMN_WIDTH = 72.dp
private val NET_SALES_COLUMN_WIDTH = 96.dp

@Composable
fun DashboardTopCategoriesWidgetCard(
    parentViewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    topCategoriesViewModel: DashboardTopCategoriesViewModel = hiltViewModel(
        creationCallback = { factory: DashboardTopCategoriesViewModel.Factory ->
            factory.create(parentViewModel)
        }
    )
) {
    topCategoriesViewModel.topCategoriesState.observeAsState().value?.let { topCategoriesState ->
        val lastUpdateState by topCategoriesViewModel.lastUpdateTopCategories.observeAsState()
        val selectedDateRange by topCategoriesViewModel.selectedDateRange.observeAsState()
        WidgetCard(
            titleResource = topCategoriesState.titleStringRes,
            menu = topCategoriesState.menu,
            button = null,
            modifier = modifier.testTag(DASHBOARD_TOP_CATEGORIES_CARD),
            isError = topCategoriesState.error != null
        ) {
            when {
                topCategoriesState.error != null -> TopCategoriesErrorView(
                    errorType = topCategoriesState.error,
                    onContactSupportClicked = parentViewModel::onContactSupportClicked,
                    onRetryClicked = topCategoriesViewModel::onRefresh
                )

                else -> DashboardTopCategoriesContent(
                    topCategoriesState = topCategoriesState,
                    selectedDateRange = selectedDateRange,
                    lastUpdateState = lastUpdateState,
                    onTabSelected = topCategoriesViewModel::onRangeChanged,
                    onEditCustomRangeTapped = topCategoriesViewModel::onEditCustomRangeTapped
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
        topCategoriesViewModel.event,
        openDatePicker = { fromDate, toDate ->
            openDatePicker(fromDate, toDate) { from, to ->
                topCategoriesViewModel.onCustomRangeSelected(StatsTimeRange(Date(from), Date(to)))
            }
        }
    )
}

@Composable
fun DashboardTopCategoriesContent(
    topCategoriesState: TopCategoriesState?,
    selectedDateRange: TopCategoriesDateRange?,
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
            topCategoriesState?.isLoading == true -> TopCategoriesLoading(
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            else -> {
                TopCategoriesContent(
                    topCategoriesState = topCategoriesState,
                    lastUpdateState = lastUpdateState
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
                is OpenCategoryProducts -> {
                    navController.navigateSafely(
                        DashboardFragmentDirections.actionDashboardToCategoryProductsFragment(
                            categoryId = event.categoryId,
                            categoryName = event.categoryName,
                            startDateMillis = event.rangeSelection.currentRange.start.time,
                            endDateMillis = event.rangeSelection.currentRange.end.time
                        )
                    )
                }

                is OpenDatePicker -> openDatePicker(event.fromDate.time, event.toDate.time)
            }
        }
        event.observe(lifecycleOwner, observer)
        onDispose {
            event.removeObserver(observer)
        }
    }
}

@Composable
private fun TopCategoriesContent(
    topCategoriesState: TopCategoriesState?,
    lastUpdateState: String?,
) {
    Column {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.category),
                style = MaterialTheme.typography.body2,
                color = colorResource(id = R.color.color_on_surface_medium_selector),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                modifier = Modifier.width(ITEMS_SOLD_COLUMN_WIDTH),
                text = stringResource(id = R.string.dashboard_top_categories_items_sold),
                style = MaterialTheme.typography.body2,
                color = colorResource(id = R.color.color_on_surface_medium_selector),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
            )
            Text(
                modifier = Modifier.width(NET_SALES_COLUMN_WIDTH),
                text = stringResource(id = R.string.dashboard_top_categories_net_sales),
                style = MaterialTheme.typography.body2,
                color = colorResource(id = R.color.color_on_surface_medium_selector),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
            )
        }
        when {
            topCategoriesState?.topCategories.isNullOrEmpty() -> TopCategoriesEmptyView()
            else -> TopCategoryCategoryList(
                topCategories = topCategoriesState.topCategories,
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
                color = colorResource(id = R.color.color_on_surface_medium_selector),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TopCategoryCategoryList(
    topCategories: List<TopPerformerCategoryUiModel>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        topCategories.forEachIndexed { index, category ->
            TopCategoryCategoryItem(
                topCategory = category,
                onItemClicked = category.onClick,
                displayDivider = index != topCategories.size - 1
            )
        }
    }
}

@Composable
private fun TopCategoriesLoading(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(5) {
            TopCategorySkeletonItem()
            Divider()
        }
    }
}

@Composable
private fun TopCategorySkeletonItem() {
    Row(
        modifier = Modifier
            .padding(top = 16.dp, bottom = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonView(
            modifier = Modifier
                .height(14.dp)
                .weight(1f)
                .padding(end = 16.dp)
        )
        SkeletonView(
            modifier = Modifier
                .height(14.dp)
                .width(50.dp)
                .padding(end = 16.dp)
        )
        SkeletonView(
            modifier = Modifier
                .height(14.dp)
                .width(60.dp)
        )
    }
}

@Composable
private fun TopCategoryCategoryItem(
    topCategory: TopPerformerCategoryUiModel,
    onItemClicked: (Long) -> Unit,
    modifier: Modifier = Modifier,
    displayDivider: Boolean
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onItemClicked(topCategory.categoryId) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = topCategory.name,
                style = MaterialTheme.typography.subtitle1,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                modifier = Modifier.width(ITEMS_SOLD_COLUMN_WIDTH),
                text = topCategory.timesOrdered,
                style = MaterialTheme.typography.subtitle1,
                textAlign = TextAlign.End
            )
            Text(
                modifier = Modifier.width(NET_SALES_COLUMN_WIDTH),
                text = topCategory.netSales,
                style = MaterialTheme.typography.subtitle1,
                textAlign = TextAlign.End
            )
        }
        if (displayDivider) {
            Divider(modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun TopCategoriesEmptyView(modifier: Modifier = Modifier) {
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
            text = stringResource(id = R.string.dashboard_top_categories_empty),
            style = MaterialTheme.typography.body2,
        )
    }
}

@Composable
private fun TopCategoriesErrorView(
    errorType: DashboardTopCategoriesViewModel.ErrorType,
    onContactSupportClicked: () -> Unit,
    onRetryClicked: () -> Unit
) {
    when (errorType) {
        DashboardTopCategoriesViewModel.ErrorType.WCAnalyticsInactive -> {
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

@LightDarkThemePreviews
@Composable
private fun TopCategoriesWidgetCardPreview() {
    val selectedDateRange = TopCategoriesDateRange(
        SelectionType.TODAY.generateSelectionData(
            referenceStartDate = Date(),
            referenceEndDate = Date(),
            calendar = Calendar.getInstance(),
            locale = Locale.getDefault(),
        ),
        customRange = null,
        dateFormatted = "Today"
    )
    val topCategoriesState = TopCategoriesState(
        topCategories = listOf(
            TopPerformerCategoryUiModel(
                categoryId = 1,
                name = "Category 1",
                timesOrdered = "10",
                netSales = "$100",
                onClick = {}
            ),
            TopPerformerCategoryUiModel(
                categoryId = 2,
                name = "Category 2",
                timesOrdered = "20",
                netSales = "$200",
                onClick = {}
            ),
            TopPerformerCategoryUiModel(
                categoryId = 3,
                name = "Category 3",
                timesOrdered = "30",
                netSales = "$300",
                onClick = {}
            ),
        ),
        isLoading = false,
        titleStringRes = DashboardWidget.Type.TOP_CATEGORIES.titleResource,
        menu = DashboardWidgetMenu(emptyList()),
    )
    Column {
        DashboardTopCategoriesContent(
            topCategoriesState = topCategoriesState,
            lastUpdateState = "Last update: 8:52 AM",
            selectedDateRange = selectedDateRange,
            onTabSelected = {},
            onEditCustomRangeTapped = {}
        )
        DashboardTopCategoriesContent(
            topCategoriesState = topCategoriesState.copy(isLoading = true),
            lastUpdateState = "Last update: 8:52 AM",
            selectedDateRange = selectedDateRange,
            onTabSelected = {},
            onEditCustomRangeTapped = {}
        )
        DashboardTopCategoriesContent(
            topCategoriesState = topCategoriesState.copy(
                error = DashboardTopCategoriesViewModel.ErrorType.Generic
            ),
            lastUpdateState = "Last update: 8:52 AM",
            selectedDateRange = selectedDateRange,
            onTabSelected = {},
            onEditCustomRangeTapped = {}
        )
        DashboardTopCategoriesContent(
            topCategoriesState = topCategoriesState.copy(topCategories = emptyList()),
            lastUpdateState = "Last update: 8:52 AM",
            selectedDateRange = selectedDateRange,
            onTabSelected = {},
            onEditCustomRangeTapped = {}
        )
    }
}
