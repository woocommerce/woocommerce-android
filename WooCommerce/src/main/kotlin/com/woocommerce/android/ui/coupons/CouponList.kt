package com.woocommerce.android.ui.coupons

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.extensions.capitalizeWords
import com.woocommerce.android.ui.coupons.CouponListViewModel.CouponListState
import com.woocommerce.android.ui.coupons.CouponListViewModel.CouponUi
import com.woocommerce.android.util.StringUtils
import java.lang.StringBuilder
import java.math.BigDecimal
import kotlin.random.Random

@Composable
fun CouponListScreen(viewModel: CouponListViewModel) {
    val couponListState by viewModel.couponsState.observeAsState(CouponListState())

    CouponListScreen(state = couponListState)
}

@Composable
fun CouponListScreen(state: CouponListState) {
    when {
        state.coupons.isNotEmpty() -> CouponList(
            coupons = state.coupons,
            currencyCode = state.currencyCode
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
fun CouponList(coupons: List<CouponUi>, currencyCode: String? = null) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier
            .background(color = MaterialTheme.colors.surface)
    ) {
        itemsIndexed(coupons) { index, coupon ->
            CouponListItem(
                coupon = coupon,
                currencyCode = currencyCode,
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
                color = MaterialTheme.colors.onSurface,
                fontSize = 20.sp
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

        val rand = Random.nextBoolean()
        CouponListExpirationLabel(rand)
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
            sb.append(
                StringUtils.getQuantityString(
                    it,
                    default = R.string.product_count_many,
                    one = R.string.product_count_one
                )
            )
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
            text = sb.toString().capitalizeWords(),
            style = MaterialTheme.typography.body2,
            color = colorResource(id = R.color.color_surface_variant),
            modifier = Modifier
                .padding(vertical = 4.dp)
        )
    }
}

@Composable
fun CouponListExpirationLabel(active: Boolean = true) {
    // todo this should check a coupon's expiration date
    // to show either "Active" or "Expired" Label
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp, 4.dp, 4.dp, 4.dp))
    ) {
        val status = if (active) {
            stringResource(id = R.string.coupon_list_item_label_active)
        } else {
            stringResource(id = R.string.coupon_list_item_label_expired)
        }

        val color = if (active) colorResource(id = R.color.woo_celadon_5) else colorResource(id = R.color.woo_gray_5)

        Text(
            text = status,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSecondary,
            modifier = Modifier
                .background(color = color)
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
            discountType = "fixed_cart",
            includedProductsCount = 1
        ),

        CouponUi(
            id = 3,
            code = "BlackFriday",
            amount = BigDecimal(5),
            discountType = "fixed_product"
        ),
    )

    CouponList(coupons = coupons, "USD")
}

@ExperimentalFoundationApi
@Preview
@Composable
fun CouponListEmptyPreview() {
    EmptyCouponList()
}
