package com.woocommerce.android.ui.coupons

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.coupons.CouponListViewModel.CouponListItem
import com.woocommerce.android.ui.coupons.CouponListViewModel.CouponListState
import com.woocommerce.android.ui.coupons.components.CouponExpirationLabel

@Composable
fun CouponListScreen(viewModel: CouponListViewModel) {
    val couponListState by viewModel.couponsState.observeAsState(CouponListState())

    CouponListScreen(
        state = couponListState,
        onCouponClick = viewModel::onCouponClick
    )
}

@Composable
fun CouponListScreen(
    state: CouponListState,
    onCouponClick: (Long) -> Unit
) {
    when {
        state.coupons.isNotEmpty() -> CouponList(
            coupons = state.coupons,
            onCouponClick = onCouponClick
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
    onCouponClick: (Long) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier
            .background(color = MaterialTheme.colors.surface)
    ) {
        itemsIndexed(coupons) { index, coupon ->
            CouponListItem(
                coupon = coupon,
                onCouponClick = onCouponClick
            )
            if (index < coupons.lastIndex) {
                Divider(
                    modifier = Modifier
                        .offset(x = 16.dp),
                    color = colorResource(id = R.color.divider_color),
                    thickness = 1.dp
                )
            }
        }
    }
}

@Composable
fun CouponListItem(
    coupon: CouponListItem,
    onCouponClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(
                enabled = true,
                onClickLabel = stringResource(id = R.string.coupon_list_view_coupon),
                role = Role.Button,
                onClick = { onCouponClick(coupon.id) }
            ),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        coupon.code?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface,
                fontSize = 20.sp
            )
        }

        CouponListItemInfo(coupon.formattedDiscount, coupon.affectedArticles)

        CouponExpirationLabel(coupon.isActive)
    }
}

@Composable
fun CouponListItemInfo(
    amount: String,
    affectedArticles: String,
) {
    Text(
        text = "$amount ${stringResource(id = R.string.coupon_list_item_label_off)} $affectedArticles",
        style = MaterialTheme.typography.body2,
        color = colorResource(id = R.color.color_surface_variant),
        fontSize = 14.sp,
        modifier = Modifier.padding(vertical = 4.dp)
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
            formattedDiscount = "USD 10.00",
            affectedArticles = "all products",
            isActive = true
        ),

        CouponListItem(
            id = 2,
            code = "10off",
            formattedDiscount = "5%",
            affectedArticles = "1 product, 2 categories",
            isActive = true
        ),

        CouponListItem(
            id = 3,
            code = "BlackFriday",
            formattedDiscount = "USD 3.00",
            affectedArticles = "all products",
            isActive = true
        ),
    )

    CouponList(coupons = coupons) {}
}

@ExperimentalFoundationApi
@Preview
@Composable
fun CouponListEmptyPreview() {
    EmptyCouponList()
}
