package com.woocommerce.android.ui.coupons.selector

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.InfiniteListHandler
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.coupons.CouponListItem
import com.woocommerce.android.ui.coupons.CouponListSkeleton
import com.woocommerce.android.ui.coupons.selector.LoadingState.Appending

@Composable
fun CouponSelectorScreen(
    modifier: Modifier = Modifier,
    state: State<CouponSelectorState?>,
    onCouponClicked: (CouponListItem) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onEmptyScreenButtonClicked: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface)
    ) {
        when {
            state.value?.coupons?.isNotEmpty() == true -> CouponSelectorList(
                coupons = state.value?.coupons ?: emptyList(),
                loadingState = state.value?.loadingState ?: LoadingState.Loading,
                onCouponClicked = onCouponClicked,
                onRefresh = onRefresh,
                onLoadMore = onLoadMore,
            )

            state.value?.loadingState == LoadingState.Loading -> CouponSelectorListSkeleton()
            else -> EmptyCouponSelectorList(onEmptyScreenButtonClicked)
        }
    }
}

@Composable
fun EmptyCouponSelectorList(
    onButtonClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(id = R.dimen.major_200)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.coupon_selector_empty_list_title),
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
            text = stringResource(id = R.string.coupon_selector_empty_list_message),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(
                start = dimensionResource(id = R.dimen.major_150),
                end = dimensionResource(id = R.dimen.major_150)
            )
        )
        Spacer(Modifier.size(dimensionResource(id = R.dimen.major_325)))
        WCColoredButton(
            onClick = onButtonClicked,
            text = stringResource(id = R.string.coupon_selector_empty_list_button),
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_100)),
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CouponSelectorList(
    coupons: List<CouponListItem>,
    loadingState: LoadingState,
    onCouponClicked: (CouponListItem) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val isRefreshing = loadingState == LoadingState.Refreshing
    val pullRefreshState = rememberPullRefreshState(isRefreshing, { onRefresh() })
    Box(Modifier.pullRefresh(pullRefreshState)) {
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
            if (loadingState == Appending) {
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

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = MaterialTheme.colors.primary,
        )
    }
}

@Composable
fun CouponSelectorListItem(
    coupon: CouponListItem,
    onCouponClicked: (CouponListItem) -> Unit,
) {
    CouponListItem(
        coupon = coupon,
        onCouponClick = onCouponClicked
    )
}

@Composable
fun CouponSelectorListSkeleton() {
    CouponListSkeleton()
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
fun CouponSelectorListItemPreview() {
    CouponSelectorListItem(
        coupon = CouponListItem(
            id = 1,
            code = "coupon1",
            summary = "This is a summary of the coupon",
            isActive = true
        )
    ) {}
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
fun CouponSelectorListPreview() {
    val coupons = listOf(
        CouponListItem(
            id = 1,
            code = "coupon1",
            summary = "This is a summary of the coupon",
            isActive = true
        ),
        CouponListItem(
            id = 2,
            code = "coupon2",
            summary = "This is a summary of the coupon",
            isActive = true
        ),
        CouponListItem(
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
        onRefresh = {}
    ) {}
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
    EmptyCouponSelectorList {}
}
