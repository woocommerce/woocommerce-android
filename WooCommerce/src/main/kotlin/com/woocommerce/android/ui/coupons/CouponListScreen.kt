package com.woocommerce.android.ui.coupons

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.InfiniteListHandler
import com.woocommerce.android.ui.coupons.CouponListViewModel.CouponListItem
import com.woocommerce.android.ui.coupons.CouponListViewModel.CouponListState
import com.woocommerce.android.ui.coupons.components.CouponExpirationLabel

@Composable
fun CouponListScreen(viewModel: CouponListViewModel) {
    val couponListState by viewModel.couponsState.observeAsState(CouponListState())

    CouponListScreen(
        state = couponListState,
        onCouponClick = viewModel::onCouponClick,
        onLoadMore = viewModel::onLoadMore
    )
}

@Composable
fun CouponListScreen(
    state: CouponListState,
    onCouponClick: (Long) -> Unit,
    onLoadMore: () -> Unit
) {
    when {
        state.coupons.isNotEmpty() -> CouponList(
            coupons = state.coupons,
            onCouponClick = onCouponClick,
            onLoadMore = onLoadMore
        )
        state.coupons.isEmpty() -> EmptyCouponList()
    }
}

@Composable
fun EmptyCouponList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.coupon_list_empty_heading),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp)
        )
        Spacer(Modifier.size(54.dp))
        Image(
            painter = painterResource(id = R.drawable.img_empty_coupon_list),
            contentDescription = null,
        )
    }
}

@Composable
fun CouponList(
    coupons: List<CouponListItem>,
    onCouponClick: (Long) -> Unit,
    onLoadMore: () -> Unit
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
                modifier = Modifier
                    .offset(x = 16.dp),
                color = colorResource(id = R.color.divider_color),
                thickness = 1.dp
            )
        }
    }

    InfiniteListHandler(listState = listState) {
        onLoadMore()
    }
}

@Composable
fun CouponListItem(
    coupon: CouponListItem,
    onCouponClick: (Long) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = true,
                onClickLabel = stringResource(id = R.string.coupon_list_view_coupon),
                role = Role.Button,
                onClick = { onCouponClick(coupon.id) }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
fun CouponListItemInfo(
    summary: String,
) {
    Text(
        text = summary,
        style = MaterialTheme.typography.caption,
        color = colorResource(id = R.color.color_on_surface_medium)
    )
}

@ExperimentalFoundationApi
@Preview
@Composable
@Suppress("MagicNumber")
fun CouponListPreview() {
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

    CouponList(coupons = coupons, {}, {})
}

@ExperimentalFoundationApi
@Preview
@Composable
fun CouponListEmptyPreview() {
    EmptyCouponList()
}
