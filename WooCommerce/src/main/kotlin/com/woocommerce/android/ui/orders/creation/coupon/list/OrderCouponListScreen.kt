package com.woocommerce.android.ui.orders.creation.coupon.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.model.Order

@Composable
fun OrderCouponListScreen(
    onNavigateBackClicked: () -> Unit = {},
    onCouponClicked: (Order.CouponLine) -> Unit = {},
    couponsState: State<List<Order.CouponLine>>,
) {
    Scaffold(
        topBar = { TopBar(onNavigateBackClicked) },
    ) { padding ->
        CouponList(padding, couponsState, onCouponClicked)
    }
}

@Composable
private fun TopBar(onNavigateBackClicked: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(id = R.string.order_creation_coupons_title)) },
        navigationIcon = {
            IconButton({ onNavigateBackClicked() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.back)
                )
            }
        },
        backgroundColor = colorResource(id = R.color.color_toolbar),
        elevation = dimensionResource(id = R.dimen.appbar_elevation),
    )
}

@Composable
private fun CouponList(
    padding: PaddingValues,
    couponsState: State<List<Order.CouponLine>>,
    onCouponClicked: (Order.CouponLine) -> Unit
) {
    LazyColumn(
        Modifier
            .padding(padding)
            .background(color = MaterialTheme.colors.surface)
    ) {
        itemsIndexed(
            items = couponsState.value,
            key = { _: Int, item: Order.CouponLine -> item.code }
        ) { index, coupon ->
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = true,
                        onClick = { onCouponClicked(coupon) },
                        role = Role.Button,
                        onClickLabel = stringResource(id = R.string.coupon_details_menu_edit),
                    )
                    .padding(dimensionResource(id = R.dimen.major_100)),
                text = coupon.code,
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface,
            )
            if (index < couponsState.value.lastIndex) {
                Divider(
                    modifier = Modifier.offset(x = dimensionResource(id = R.dimen.major_100)),
                    color = colorResource(id = R.color.divider_color),
                    thickness = dimensionResource(id = R.dimen.minor_10)
                )
            }
        }
    }
}

@Composable
@Preview
private fun CouponListPreview() {
    val state = remember {
        mutableStateOf(listOf(Order.CouponLine(code = "coupon1"), Order.CouponLine(code = "coupon2")))
    }
    CouponList(
        padding = PaddingValues(),
        couponsState = state,
        onCouponClicked = {}
    )
}

@Composable
@Preview
private fun TopBarPreview() {
    TopBar(onNavigateBackClicked = {})
}
