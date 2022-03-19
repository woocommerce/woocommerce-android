package com.woocommerce.android.ui.coupons

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.coupons.CouponListViewModel.CouponListState
import com.woocommerce.android.ui.coupons.CouponListViewModel.CouponUi
import java.lang.StringBuilder
import java.math.BigDecimal

@Composable
fun CouponListContainer(viewModel: CouponListViewModel) {
    val couponListState by viewModel.couponsState.observeAsState(CouponListState())

    CouponListContainer(state = couponListState)
}

@Composable
fun CouponListContainer(state: CouponListState) {
    when {
        state.coupons.isNotEmpty() -> CouponList(
            coupons = state.coupons,
            currencyCode = state.currencyCode
        )
    }
}

@Composable
fun CouponList(coupons: List<CouponUi>, currencyCode: String?) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier
            .background(color = colorResource(id = R.color.color_surface))
    ) {
        itemsIndexed(coupons) { index, coupon ->
            CouponListItem(
                coupon = coupon,
                currencyCode = currencyCode,
            )
            Divider(
                modifier = Modifier
                    .offset(x = 16.dp),
                color = colorResource(id = R.color.divider_color),
                thickness = 1.dp
            )
        }
    }
}

@Composable
fun CouponListItem(coupon: CouponUi, currencyCode: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        coupon.code?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.h6,
                color = colorResource(id = R.color.color_on_surface)
            )
        }

        CouponListItemInfo(
            coupon.amount,
            coupon.discountType,
            currencyCode,
            coupon.includedProductsCount,
            coupon.excludedProductsCount,
            coupon.includedCategoryCount,
        )

        CouponListExpirationLabel()
    }
}

@Composable
fun CouponListItemInfo(
    amount: BigDecimal?,
    discountType: String?,
    currencyCode: String?,
    includedProductsCount: Int? = null,
    excludedProductsCount: Int? = null,
    includedCategoryCount: Int? = null,
) {
    if (amount == null || discountType == null) {
        // Show nothing if the amount or discount type is unclear.
        return
    } else {
        val sb = StringBuilder()
        val amountText = amount.toString()
        when (discountType) {
            "percent" -> sb.append("$amountText% ")
            else -> {
                currencyCode?.let {
                    sb.append("$currencyCode $amountText ")
                }
            }
        }
        sb.append(stringResource(id = R.string.coupon_list_item_label_off))
        sb.append(" ")

        if (includedProductsCount == null && excludedProductsCount == null) {
            sb.append(stringResource(id = R.string.coupon_list_item_label_all_products))
        }
        includedProductsCount?.let {
            sb.append(includedProductsCount)
            sb.append(" ")
            sb.append(stringResource(id = R.string.products))
        }
        includedCategoryCount?.let {
            if (includedProductsCount != null) {
                sb.append(", ")
            }
            sb.append(includedCategoryCount)
            sb.append(" ")
            sb.append(stringResource(id = R.string.product_categories))
        }

        Text(
            text = sb.toString(),
            style = MaterialTheme.typography.body2,
            color = colorResource(id = R.color.color_surface_variant),
            modifier = Modifier
                .padding(vertical = 4.dp)
        )
    }
}

@Composable
fun CouponListExpirationLabel() {
    // todo this should check a coupon's expiration date
    // to show either "Active" or "Expired" Label
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp, 4.dp, 4.dp, 4.dp))
    ) {
        Text(
            text = stringResource(id = R.string.coupon_list_item_label_active),
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .background(color = colorResource(id = R.color.woo_celadon_5))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
@Suppress("MagicNumber")
fun CouponListPreview() {
    val coupons = listOf(
        CouponUi(
            id = 1,
            code = "ABCDE",
            amount = BigDecimal(25),
            discountType = "percent",
            includedProductsCount = 5,
            includedCategoryCount = 4
        ),

        CouponUi(
            id = 2,
            code = "10off",
            amount = BigDecimal(10),
            discountType = "fixed_cart"
        ),

        CouponUi(
            id = 3,
            code = "BlackFriday",
            amount = BigDecimal(5),
            discountType = "fixed_product"
        ),
    )

    CouponList(coupons = coupons, "$")
}
