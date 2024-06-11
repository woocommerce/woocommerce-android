package com.woocommerce.android.ui.orders.creation.shipping

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.model.ShippingMethod
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun OrderShippingMethodsScreen(
    viewModel: OrderShippingMethodsViewModel,
    modifier: Modifier = Modifier
) {
    val viewState by viewModel.viewState.collectAsState()
    OrderShippingMethodsScreen(
        viewState = viewState,
        onMethodSelected = viewModel::onMethodSelected,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
        modifier = modifier
    )
}

@Composable
fun OrderShippingMethodsScreen(
    viewState: OrderShippingMethodsViewModel.ViewState,
    onMethodSelected: (method: OrderShippingMethodsViewModel.ShippingMethodUI) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (viewState) {
        OrderShippingMethodsViewModel.ViewState.Error -> {
            ErrorMessage(
                onRetry = onRetry,
                modifier = modifier.padding(16.dp)
            )
        }

        OrderShippingMethodsViewModel.ViewState.Loading -> {
            OrderShippingMethodsListSkeleton(modifier = modifier.padding(16.dp))
        }

        is OrderShippingMethodsViewModel.ViewState.ShippingMethodsState -> {
            OrderShippingMethodsList(
                methods = viewState.methods,
                onMethodSelected = onMethodSelected,
                isRefreshing = viewState.isRefreshing,
                onRefresh = onRefresh,
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OrderShippingMethodsList(
    methods: List<OrderShippingMethodsViewModel.ShippingMethodUI>,
    onMethodSelected: (method: OrderShippingMethodsViewModel.ShippingMethodUI) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pullRefreshState = rememberPullRefreshState(isRefreshing, { onRefresh() })
    Box(Modifier.pullRefresh(pullRefreshState)) {
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            items(methods, key = { item -> item.method.id }) { method ->
                SelectableShippingMethod(
                    method = method,
                    modifier = Modifier.clickable { onMethodSelected(method) }
                )
            }
        }
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = MaterialTheme.colors.primary,
        )
    }
}

@Composable
fun SelectableShippingMethod(
    method: OrderShippingMethodsViewModel.ShippingMethodUI,
    modifier: Modifier = Modifier
) {
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp, end = 8.dp)
        ) {
            Text(
                text = method.method.title,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(2f, fill = true)
            )
            AnimatedVisibility(visible = method.isSelected) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_done_secondary),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)

                )
            }
        }
        Divider()
    }
}

@Preview
@Composable
fun SelectableShippingMethodPreview(@PreviewParameter(IsSelectedProvider::class) isSelected: Boolean) {
    val method = OrderShippingMethodsViewModel.ShippingMethodUI(
        method = ShippingMethod(
            id = "other",
            title = "Other"
        ),
        isSelected = isSelected
    )
    WooThemeWithBackground {
        SelectableShippingMethod(method = method)
    }
}

class IsSelectedProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(true, false)
}

@Composable
fun OrderShippingMethodsListSkeleton(modifier: Modifier) {
    val numberOfInboxSkeletonRows = 5
    LazyColumn(modifier) {
        repeat(numberOfInboxSkeletonRows) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_50)),
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp, end = 8.dp)
                ) {
                    SkeletonView(
                        dimensionResource(id = R.dimen.skeleton_text_large_width),
                        dimensionResource(id = R.dimen.major_100)
                    )
                }
                Divider()
            }
        }
    }
}

@Preview
@Composable
fun OrderShippingMethodsSkeletonPreview() {
    WooThemeWithBackground {
        OrderShippingMethodsListSkeleton(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        )
    }
}

@Composable
private fun ErrorMessage(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimensionResource(id = R.dimen.major_200)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Image(
            painter = painterResource(id = R.drawable.img_woo_generic_error),
            contentDescription = null,
        )
        Spacer(Modifier.size(dimensionResource(id = R.dimen.major_200)))
        Text(
            text = stringResource(id = R.string.order_creation_shipping_methods_error),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.padding(
                start = dimensionResource(id = R.dimen.major_150),
                end = dimensionResource(id = R.dimen.major_150)
            )
        )

        Spacer(Modifier.size(dimensionResource(id = R.dimen.minor_100)))
        WCColoredButton(onClick = { onRetry() }) {
            Text(text = stringResource(id = R.string.retry))
        }

        Spacer(Modifier.weight(1f))
    }
}

@Preview
@Composable
fun ErrorMessagePreview() {
    WooThemeWithBackground {
        ErrorMessage(
            onRetry = {},
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        )
    }
}
