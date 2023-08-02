package com.woocommerce.android.ui.coupons.selector

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.InfiniteListHandler
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.coupons.components.CouponExpirationLabel

@Composable
fun CouponSelectorScreen(viewModel: CouponSelectorViewModel) {
    val viewState by viewModel.couponSelectorState.observeAsState(CouponSelectorState())
    BackHandler(onBack = viewModel::onNavigateBack)
    viewState.let { state ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(id = R.string.order_creation_select_coupon)) },
                    backgroundColor = colorResource(id = R.color.color_toolbar),
                    elevation = 0.dp
                )
            },
        ) { padding ->
            CouponSelectorScreen(
                modifier = Modifier.padding(padding),
                state = state,
                onCouponClicked = viewModel::onCouponClicked,
                onRefresh = viewModel::onRefresh,
                onLoadMore = viewModel::onLoadMore,
            )
        }
    }
}

@Composable
fun CouponSelectorScreen(
    modifier: Modifier = Modifier,
    state: CouponSelectorState,
    onCouponClicked: (Long) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface)
    ) {
        when {
            state.coupons.isNotEmpty() -> CouponSelectorList(
                coupons = state.coupons,
                loadingState = state.loadingState,
                onCouponClicked = onCouponClicked,
                onRefresh = onRefresh,
                onLoadMore = onLoadMore,
            )

            state.loadingState == LoadingState.Loading -> CouponSelectorListSkeleton()
            state.isSearchOpen -> CouponSelectorEmptySearch(searchQuery = state.searchQuery.orEmpty())
            else -> EmptyCouponSelectorList()
        }
    }
}

@Composable
fun EmptyCouponSelectorList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(id = R.dimen.major_200)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.order_creation_coupons_empty_list_title),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(
                start = dimensionResource(id = R.dimen.major_150),
                end = dimensionResource(id = R.dimen.major_150)
            )
        )
        Spacer(Modifier.size(dimensionResource(id = R.dimen.major_325)))
        Image(
            painter = painterResource(id = R.drawable.img_empty_coupon_list),
            contentDescription = null,
        )
        Spacer(Modifier.size(dimensionResource(id = R.dimen.major_200)))
        Text(
            text = stringResource(id = R.string.order_creation_coupons_empty_list_message),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(
                start = dimensionResource(id = R.dimen.major_150),
                end = dimensionResource(id = R.dimen.major_150)
            )
        )
        Spacer(Modifier.size(dimensionResource(id = R.dimen.major_325)))
        WCColoredButton(
            onClick = {},
            text = stringResource(id = R.string.order_creation_coupons_empty_list_button),
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_100)),
        )
    }
}

@Composable
fun CouponSelectorList(
    coupons: List<CouponSelectorItem>,
    loadingState: LoadingState,
    onCouponClicked: (Long) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
) {
    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = loadingState == LoadingState.Refreshing),
        onRefresh = onRefresh,
        indicator = { state, refreshTrigger ->
            SwipeRefreshIndicator(
                state = state,
                refreshTriggerDistance = refreshTrigger,
                contentColor = MaterialTheme.colors.primary,
            )
        }
    ) {
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .background(color = MaterialTheme.colors.surface)
        ) {
            itemsIndexed(coupons) { _, coupons ->
                CouponSelectorListItem(coupons, onCouponClicked)
                Divider(
                    modifier = Modifier.offset(x = dimensionResource(id = R.dimen.major_100)),
                    color = colorResource(id = R.color.divider_color),
                    thickness = dimensionResource(id = R.dimen.minor_10)
                )
            }
            if (loadingState == LoadingState.Appending) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth()
                            .padding(vertical = dimensionResource(id = R.dimen.minor_100))
                    )
                }
            }
        }

        InfiniteListHandler(listState = listState) {
            onLoadMore()
        }
    }
}

@Composable
fun CouponSelectorListItem(
    coupon: CouponSelectorItem,
    onCouponClicked: (Long) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_50)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = true,
                role = Role.Button,
                onClick = { onCouponClicked(coupon.id) }
            )
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        coupon.code?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface,
            )
        }

        CouponSelectorListItemInfo(summary = coupon.summary)
        CouponExpirationLabel(coupon.isActive)
    }
}

@Composable
fun CouponSelectorListItemInfo(summary: String) {
    Text(
        text = summary,
        style = MaterialTheme.typography.caption,
        color = colorResource(id = R.color.color_on_surface_medium)
    )
}

@Composable
fun CouponSelectorListSkeleton() {
    val numberOfInboxSkeletonRows = 10
    LazyColumn(Modifier.background(color = MaterialTheme.colors.surface)) {
        repeat(numberOfInboxSkeletonRows) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_50)),
                    modifier = Modifier.padding(
                        horizontal = dimensionResource(id = R.dimen.major_100),
                        vertical = dimensionResource(id = R.dimen.minor_100)
                    )
                ) {
                    SkeletonView(
                        dimensionResource(id = R.dimen.skeleton_text_medium_width),
                        dimensionResource(id = R.dimen.major_125)
                    )
                    SkeletonView(
                        dimensionResource(id = R.dimen.skeleton_text_large_width),
                        dimensionResource(id = R.dimen.major_100)
                    )
                    SkeletonView(
                        dimensionResource(id = R.dimen.skeleton_text_small_width),
                        dimensionResource(id = R.dimen.major_125)
                    )
                }
                Divider(
                    modifier = Modifier
                        .offset(x = dimensionResource(id = R.dimen.major_100)),
                    color = colorResource(id = R.color.divider_color),
                    thickness = dimensionResource(id = R.dimen.minor_10)
                )
            }
        }
    }
}

@Composable
fun CouponSelectorEmptySearch(searchQuery: String) {
    if (searchQuery.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(id = R.dimen.major_200)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.empty_message_with_search, searchQuery),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(
                start = dimensionResource(id = R.dimen.major_150),
                end = dimensionResource(id = R.dimen.major_150)
            )
        )
        Spacer(Modifier.size(dimensionResource(id = R.dimen.major_325)))
        Image(
            painter = painterResource(id = R.drawable.img_empty_search),
            contentDescription = null,
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
fun CouponSelectorListItemPreview() {
    CouponSelectorListItem(
        coupon = CouponSelectorItem(
            id = 1,
            code = "coupon1",
            summary = "This is a summary of the coupon",
            isActive = true
        ),
        onCouponClicked = {}
    )
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
fun CouponSelectorListPreview() {
    val coupons = listOf(
        CouponSelectorItem(
            id = 1,
            code = "coupon1",
            summary = "This is a summary of the coupon",
            isActive = true
        ),
        CouponSelectorItem(
            id = 2,
            code = "coupon2",
            summary = "This is a summary of the coupon",
            isActive = true
        ),
        CouponSelectorItem(
            id = 3,
            code = "coupon3",
            summary = "This is a summary of the coupon",
            isActive = true
        ),
    )
    CouponSelectorList(
        coupons = coupons,
        onCouponClicked = {},
        loadingState = LoadingState.Idle,
        onRefresh = {},
        onLoadMore = {}
    )
}

@Preview()
@Composable
fun CouponSelectorListSkeletonPreview() {
    CouponSelectorListSkeleton()
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
fun CouponSelectorEmptyListPreview() {
    EmptyCouponSelectorList()
}
