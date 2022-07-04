package com.woocommerce.android.ui.coupons

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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.InfiniteListHandler
import com.woocommerce.android.ui.coupons.CouponListViewModel.CouponListItem
import com.woocommerce.android.ui.coupons.CouponListViewModel.CouponListState
import com.woocommerce.android.ui.coupons.CouponListViewModel.LoadingState
import com.woocommerce.android.ui.coupons.components.CouponExpirationLabel

@Composable
fun CouponListScreen(viewModel: CouponListViewModel) {
    val couponListState by viewModel.couponsState.observeAsState(CouponListState())

    CouponListScreen(
        state = couponListState,
        onCouponClick = viewModel::onCouponClick,
        onRefresh = viewModel::onRefresh,
        onLoadMore = viewModel::onLoadMore
    )
}

@Composable
fun CouponListScreen(
    state: CouponListState,
    onCouponClick: (Long) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit
) {
    when {
        state.coupons.isNotEmpty() -> CouponList(
            coupons = state.coupons,
            loadingState = state.loadingState,
            onCouponClick = onCouponClick,
            onRefresh = onRefresh,
            onLoadMore = onLoadMore
        )
        state.loadingState == LoadingState.Loading -> CouponListSkeleton()
        state.isSearchOpen -> SearchEmptyList(searchQuery = state.searchQuery.orEmpty())
        else -> EmptyCouponList()
    }
}

@Composable
private fun EmptyCouponList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(id = R.dimen.major_200)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.coupon_list_empty_heading),
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
    }
}

@Composable
private fun CouponList(
    coupons: List<CouponListItem>,
    loadingState: LoadingState,
    onCouponClick: (Long) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit
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
            itemsIndexed(coupons) { _, coupon ->
                CouponListItem(
                    coupon = coupon,
                    onCouponClick = onCouponClick
                )
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
private fun CouponListItem(
    coupon: CouponListItem,
    onCouponClick: (Long) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_50)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = true,
                onClickLabel = stringResource(id = R.string.coupon_list_view_coupon),
                role = Role.Button,
                onClick = { onCouponClick(coupon.id) }
            )
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.minor_100)
            ),
    ) {
        coupon.code?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface,
            )
        }

        CouponListItemInfo(coupon.summary)

        CouponExpirationLabel(coupon.isActive)
    }
}

@Composable
private fun CouponListItemInfo(
    summary: String,
) {
    Text(
        text = summary,
        style = MaterialTheme.typography.caption,
        color = colorResource(id = R.color.color_on_surface_medium)
    )
}

@Composable
private fun CouponListSkeleton() {
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
private fun SearchEmptyList(searchQuery: String) {
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

@Preview
@Composable
private fun CouponListPreview() {
    val coupons = listOf(
        CouponListItem(
            id = 1,
            code = "ABCDE",
            summary = "USD 10.00 off all products",
            isActive = true
        ),

        CouponListItem(
            id = 2,
            code = "10off",
            summary = "5% off 1 product, 2 categories",
            isActive = true
        ),

        CouponListItem(
            id = 3,
            code = "BlackFriday",
            summary = "USD 3.00 off all products",
            isActive = true
        ),
    )

    CouponList(coupons = coupons, loadingState = LoadingState.Idle, {}, {}, {})
}

@Preview
@Composable
private fun CouponListEmptyPreview() {
    EmptyCouponList()
}

@Preview
@Composable
private fun CouponListSkeletonPreview() {
    CouponListSkeleton()
}
