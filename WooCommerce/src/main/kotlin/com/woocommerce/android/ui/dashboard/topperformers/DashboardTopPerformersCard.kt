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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.ProductThumbnail
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.compose.viewModelWithFactory
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.TopPerformerProductUiModel
import com.woocommerce.android.ui.dashboard.topperformers.DashboardTopPerformersViewModel.Factory
import com.woocommerce.android.ui.dashboard.topperformers.DashboardTopPerformersViewModel.OpenTopPerformer
import com.woocommerce.android.ui.dashboard.topperformers.DashboardTopPerformersViewModel.TopPerformersState
import com.woocommerce.android.ui.products.details.ProductDetailFragment.Mode.ShowProduct
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event

@Composable
fun DashboardTopPerformersCard(
    parentViewModel: DashboardViewModel,
    viewModel: DashboardTopPerformersViewModel = viewModelWithFactory<DashboardTopPerformersViewModel, Factory>(
        creationCallback = {
            it.create(parentViewModel)
        }
    )
) {
    val topPerformersState by viewModel.topPerformersState.observeAsState()
    val lastUpdateState by viewModel.lastUpdateTopPerformers.observeAsState()
    TopPerformersCard(topPerformersState, lastUpdateState)
    HandleEvents(viewModel.event)
}

@Composable
private fun HandleEvents(event: LiveData<Event>) {
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(event, navController, lifecycleOwner) {
        val observer = Observer { event: Event ->
            when (event) {
                is OpenTopPerformer -> navController.navigateSafely(
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
private fun TopPerformersCard(
    topPerformersState: TopPerformersState?,
    lastUpdateState: String?
) {
    Card(
        modifier = Modifier
            .padding(top = 16.dp, bottom = 16.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(0.dp)
    ) {
        if (topPerformersState?.isLoading == true) {
            TopPerformersLoading(modifier = Modifier.padding(16.dp))
        } else {
            TopPerformersContent(topPerformersState, lastUpdateState)
        }
    }
}

@Composable
private fun TopPerformersContent(
    topPerformersState: TopPerformersState?,
    lastUpdateState: String?
) {
    Column {
        Text(
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
            text = stringResource(id = R.string.dashboard_top_performers_title),
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.product),
                style = MaterialTheme.typography.body2,
                color = colorResource(id = R.color.color_on_surface_medium_selector)
            )
            Text(
                text = stringResource(id = R.string.dashboard_top_performers_items_sold),
                style = MaterialTheme.typography.body2,
                color = colorResource(id = R.color.color_on_surface_medium_selector)
            )
        }
        when {
            topPerformersState?.isError == true -> TopPerformersErrorView()
            topPerformersState?.topPerformers.isNullOrEmpty() -> TopPerformersEmptyView()
            else -> TopPerformerProductList(
                topPerformers = topPerformersState?.topPerformers!!
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
private fun TopPerformerProductList(
    topPerformers: List<TopPerformerProductUiModel>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        topPerformers.forEachIndexed { index, product ->
            TopPerformerProductItem(topPerformer = product, onItemClicked = product.onClick)
            if (index < topPerformers.size - 1) {
                Divider()
            }
        }
    }
}

@Composable
private fun TopPerformersLoading(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SkeletonView(
            modifier = Modifier
                .height(18.dp)
                .width(200.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            SkeletonView(
                modifier = Modifier
                    .height(16.dp)
                    .width(80.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            SkeletonView(
                modifier = Modifier
                    .height(16.dp)
                    .width(50.dp)
            )
        }
        repeat(5) {
            TopPerformerSkeletonItem()
            Divider()
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
        SkeletonView(
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
            SkeletonView(
                modifier = Modifier
                    .height(14.dp)
                    .width(180.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonView(
                modifier = Modifier
                    .height(12.dp)
                    .width(150.dp)
            )
        }
        SkeletonView(
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onItemClicked(topPerformer.productId) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProductThumbnail(
            imageUrl = topPerformer.imageUrl ?: "",
            contentDescription = stringResource(id = R.string.product_image_content_description),
        )
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            Text(
                text = topPerformer.name,
                style = MaterialTheme.typography.subtitle1,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = topPerformer.netSales,
                style = MaterialTheme.typography.body2,
                color = colorResource(id = R.color.color_on_surface_medium_selector)
            )
        }
        Text(
            text = topPerformer.timesOrdered,
            style = MaterialTheme.typography.subtitle1,
        )
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
            painter = painterResource(id = R.drawable.img_top_performers_empty),
            contentDescription = "",
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = stringResource(id = R.string.dashboard_top_performers_empty),
            style = MaterialTheme.typography.body2,
        )
    }
}

@Composable
private fun TopPerformersErrorView() {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_top_performers_empty),
            contentDescription = "",
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = stringResource(id = R.string.dashboard_top_performers_empty),
            style = MaterialTheme.typography.body2,
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun TopPerformersCardPreview() {
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
        isError = false
    )
    Column {
        TopPerformersCard(
            topPerformersState = topPerformersState,
            lastUpdateState = "Last update: 8:52 AM"
        )
        TopPerformersCard(
            topPerformersState = topPerformersState.copy(isLoading = true),
            lastUpdateState = "Last update: 8:52 AM"
        )
        TopPerformersCard(
            topPerformersState = topPerformersState.copy(isError = true),
            lastUpdateState = "Last update: 8:52 AM"
        )
        TopPerformersCard(
            topPerformersState = topPerformersState.copy(topPerformers = emptyList()),
            lastUpdateState = "Last update: 8:52 AM"
        )
    }
}
