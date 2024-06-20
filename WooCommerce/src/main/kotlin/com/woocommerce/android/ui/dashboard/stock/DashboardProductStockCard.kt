package com.woocommerce.android.ui.dashboard.stock

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.ProductThumbnail
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.compose.viewModelWithFactory
import com.woocommerce.android.ui.dashboard.DashboardFilterableCardHeader
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.WCAnalyticsNotAvailableErrorView
import com.woocommerce.android.ui.dashboard.WidgetCard
import com.woocommerce.android.ui.dashboard.WidgetError
import com.woocommerce.android.ui.dashboard.defaultHideMenuEntry
import com.woocommerce.android.ui.dashboard.stock.DashboardProductStockViewModel.OpenProductDetail
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.details.ProductDetailFragment.Mode.ShowProduct
import com.woocommerce.android.ui.products.stock.ProductStockItem
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event

@Composable
fun DashboardProductStockCard(
    parentViewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    viewModel: DashboardProductStockViewModel = viewModelWithFactory { f: DashboardProductStockViewModel.Factory ->
        f.create(parentViewModel = parentViewModel)
    }
) {
    HandleEvents(viewModel.event)

    viewModel.productStockState.observeAsState().value?.let { viewState ->
        DashboardProductStockCard(
            viewState = viewState,
            onHideClicked = { parentViewModel.onHideWidgetClicked(DashboardWidget.Type.STOCK) },
            onFilterSelected = viewModel::onFilterSelected,
            onProductClicked = viewModel::onProductClicked,
            onRetryClicked = viewModel::onRetryClicked,
            onContactSupportClicked = parentViewModel::onContactSupportClicked,
            modifier = modifier
        )
    }
}

@Composable
private fun HandleEvents(event: LiveData<Event>) {
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(event, navController, lifecycleOwner) {
        val observer = Observer { event: Event ->
            when (event) {
                is OpenProductDetail -> navController.navigateSafely(
                    NavGraphMainDirections.actionGlobalProductDetailFragment(
                        mode = ShowProduct(event.productId),
                        isTrashEnabled = false
                    )
                )
            }
        }
        event.observe(lifecycleOwner, observer)
        onDispose {
            event.removeObserver(observer)
        }
    }
}

@Composable
private fun DashboardProductStockCard(
    viewState: DashboardProductStockViewModel.ViewState,
    onHideClicked: () -> Unit,
    onFilterSelected: (ProductStockStatus) -> Unit,
    onProductClicked: (ProductStockItem) -> Unit,
    onContactSupportClicked: () -> Unit,
    onRetryClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WidgetCard(
        titleResource = DashboardWidget.Type.STOCK.titleResource,
        menu = DashboardWidgetMenu(
            listOf(
                DashboardWidget.Type.STOCK.defaultHideMenuEntry(onHideClicked)
            )
        ),
        isError = viewState is DashboardProductStockViewModel.ViewState.Error,
        modifier = modifier
    ) {
        when (viewState) {
            is DashboardProductStockViewModel.ViewState.Loading -> {
                ProductStockLoading(
                    selectedFilter = viewState.selectedFilter,
                    onFilterSelected = onFilterSelected,
                )
            }

            is DashboardProductStockViewModel.ViewState.Success -> {
                ProductStockCardContent(
                    selectedFilter = viewState.selectedFilter,
                    productStockItems = viewState.productStockItems,
                    onFilterSelected = onFilterSelected,
                    onProductClicked = onProductClicked,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            DashboardProductStockViewModel.ViewState.Error.WCAnalyticsDisabled -> {
                WCAnalyticsNotAvailableErrorView(
                    title = stringResource(id = R.string.dashboard_product_stock_wcanalytics_inactive_title),
                    onContactSupportClick = onContactSupportClicked
                )
            }

            DashboardProductStockViewModel.ViewState.Error.Generic -> {
                WidgetError(
                    onRetryClicked = onRetryClicked,
                    onContactSupportClicked = onContactSupportClicked
                )
            }
        }
    }
}

@Composable
private fun ProductStockLoading(
    selectedFilter: ProductStockStatus,
    onFilterSelected: (ProductStockStatus) -> Unit
) {
    Column {
        Header(selectedFilter, onFilterSelected)
        repeat(3) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonView(
                    modifier = Modifier
                        .height(42.dp)
                        .width(42.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SkeletonView(width = 240.dp, height = 16.dp)
                    SkeletonView(width = 140.dp, height = 12.dp)
                }
                SkeletonView(width = 20.dp, height = 14.dp)
            }
        }
    }
}

@Composable
private fun ProductStockCardContent(
    selectedFilter: ProductStockStatus,
    productStockItems: List<ProductStockItem>,
    onFilterSelected: (ProductStockStatus) -> Unit,
    onProductClicked: (ProductStockItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Header(selectedFilter, onFilterSelected)
        Row(modifier = Modifier.padding(16.dp)) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.dashboard_product_stock_products),
                style = MaterialTheme.typography.body2,
                color = colorResource(id = R.color.color_on_surface_medium_selector),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(id = R.string.dashboard_product_stock_levels),
                style = MaterialTheme.typography.body2,
                color = colorResource(id = R.color.color_on_surface_medium_selector),
                fontWeight = FontWeight.SemiBold,
            )
        }
        when {
            productStockItems.isEmpty() -> StockEmptyView()
            else -> productStockItems.forEachIndexed { index, product ->
                ProductStockRow(
                    product = product,
                    onItemClicked = onProductClicked,
                    displayDivider = index != productStockItems.size - 1
                )
            }
        }
    }
}

@Composable
fun StockEmptyView(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_products),
            contentDescription = null,
            modifier = Modifier
                .sizeIn(maxWidth = 160.dp, maxHeight = 160.dp)
                .padding(vertical = 16.dp)
        )

        Text(
            text = stringResource(id = R.string.dashboard_product_stock_empty_products),
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ProductStockRow(
    product: ProductStockItem,
    onItemClicked: (ProductStockItem) -> Unit,
    displayDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onItemClicked(product) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        ProductThumbnail(
            imageUrl = product.imageUrl ?: "",
            contentDescription = stringResource(id = R.string.product_image_content_description),
            modifier = Modifier.padding(top = 12.dp)
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    text = product.name,
                    style = MaterialTheme.typography.subtitle1,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.stockQuantity.toString(),
                    style = MaterialTheme.typography.subtitle1,
                    color = colorResource(id = R.color.color_error)
                )
            }
            Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = when {
                    product.itemsSold == 0 -> stringResource(R.string.dashboard_product_stock_no_sales_last_30_days)
                    else -> stringResource(R.string.dashboard_product_stock_sales_last_30_days, product.itemsSold)
                },
                style = MaterialTheme.typography.body2,
                color = colorResource(id = R.color.color_on_surface_medium_selector)
            )
            if (displayDivider) {
                Divider(modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun Header(
    selectedFilter: ProductStockStatus,
    onFilterSelected: (ProductStockStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        DashboardFilterableCardHeader(
            title = stringResource(id = R.string.dashboard_product_stock_status_header_title),
            currentFilter = selectedFilter,
            filterList = DashboardProductStockViewModel.supportedFilters,
            onFilterSelected = onFilterSelected,
            mapper = { stringResource(id = it.stringResource) }
        )
        Divider()
    }
}
