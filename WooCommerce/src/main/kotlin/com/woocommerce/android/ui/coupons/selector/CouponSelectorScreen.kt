package com.woocommerce.android.ui.coupons.selector

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.woocommerce.android.R
import com.woocommerce.android.ui.coupons.components.CouponExpirationLabel

@Composable
fun CouponSelectorScreen(viewModel: CouponSelectorViewModel) {

}


@Composable
fun CouponSelectorList(
    coupons: List<CouponSelectorItem>,
    onCouponSelected: (Long) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_50)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        coupons.forEach { coupon ->
            CouponSelectorListItem(coupon, onCouponSelected)
        }
    }
}

@Composable
fun CouponSelectorListItem(
    coupon: CouponSelectorItem,
    onCouponSelected: (Long) -> Unit
) {
    Column (
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_50)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = true,
                        role = Role.Button,
                        onClick = { onCouponSelected(coupon.id) })
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

@Preview
@Composable
fun CouponSelectorListItemPreview() {
    CouponSelectorListItem(
        coupon = CouponSelectorItem(
            id = 1,
            code = "coupon1",
            summary = "This is a summary of the coupon",
            isActive = true
        ),
        onCouponSelected = {}
    )
}

@Preview
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
    CouponSelectorList(coupons = coupons, onCouponSelected = {})
}
