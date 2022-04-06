package com.woocommerce.android.ui.coupons.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.coupons.details.CouponDetailsViewModel.*
import com.woocommerce.android.ui.coupons.details.CouponDetailsViewModel.CouponPerformanceState.Loading
import com.woocommerce.android.ui.coupons.details.CouponDetailsViewModel.CouponPerformanceState.Success

@Composable
fun CouponDetailsScreen(viewModel: CouponDetailsViewModel) {
    val couponSummaryState by viewModel.couponState.observeAsState(CouponDetailsState())

    CouponDetailsScreen(state = couponSummaryState)
}

@Composable
fun CouponDetailsScreen(state: CouponDetailsState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        state.coupon?.let { coupon ->
            CouponSummaryHeading(
                code = coupon.code,
                isActive = true
            )
            CouponSummarySection(coupon)
        }
        state.couponPerformanceState?.let {
            CouponPerformanceSection(it)
        }
    }
}

@Composable
fun CouponSummaryHeading(
    code: String?,
    isActive: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        code?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.h1,
                color = MaterialTheme.colors.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        CouponSummaryExpirationLabel(isActive)
    }
}

@Composable
fun CouponSummaryExpirationLabel(isActive: Boolean) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp, 4.dp, 4.dp, 4.dp))
            .padding(vertical = 4.dp)
    ) {
        val status = if (isActive) {
            stringResource(id = R.string.coupon_list_item_label_active)
        } else {
            stringResource(id = R.string.coupon_list_item_label_expired)
        }

        val color = if (isActive) colorResource(id = R.color.woo_celadon_5) else colorResource(id = R.color.woo_gray_5)

        Text(
            text = status,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSecondary,
            modifier = Modifier
                .background(color = color)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun CouponSummarySection(coupon: CouponUi) {
    Surface(
        elevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = stringResource(id = R.string.coupon_summary_heading),
                style = MaterialTheme.typography.h2,
                color = MaterialTheme.colors.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            CouponDetailsItemInfo(
                amount = coupon.formattedDiscount,
                affectedArticles = coupon.affectedArticles
            )

            Spacer(modifier = Modifier.height(24.dp))

            CouponDetailsSpendingInfo(coupon.formattedSpendingInfo)

            /* Hardcoded for design work purposes */
            Text(
                text = "Expires August 4, 2022",
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun CouponDetailsItemInfo(
    amount: String,
    affectedArticles: String
) {
    Text(
        text = "$amount ${stringResource(id = R.string.coupon_list_item_label_off)} $affectedArticles",
        style = MaterialTheme.typography.body2,
        color = MaterialTheme.colors.onSurface,
        fontSize = 20.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun CouponDetailsSpendingInfo(formattedSpendingInfo: String) {
    Text(
        style = MaterialTheme.typography.body1,
        text = formattedSpendingInfo,
        fontSize = 20.sp
    )
}

// todo use actual data instead of hardcoded value
@Composable
private fun CouponPerformanceSection(couponPerformanceState: CouponPerformanceState) {
    Surface(
        elevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = stringResource(id = string.coupon_summary_performance_heading),
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row {
                CouponPerformanceCount(
                    couponPerformanceState = couponPerformanceState,
                    modifier = Modifier.weight(1f)
                )

                CouponPerformanceAmount(
                    couponPerformanceState = couponPerformanceState,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CouponPerformanceCount(
    couponPerformanceState: CouponPerformanceState,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        Text(
            text = stringResource(id = string.coupon_summary_performance_discounted_order_heading),
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = color.color_surface_variant)
        )

        Text(
            text = couponPerformanceState.ordersCount?.toString().orEmpty(),
            style = MaterialTheme.typography.h5,
            color = MaterialTheme.colors.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CouponPerformanceAmount(
    couponPerformanceState: CouponPerformanceState,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        Text(
            text = stringResource(id = string.coupon_summary_performance_amount_heading),
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = color.color_surface_variant)
        )
        when (couponPerformanceState) {
            is Loading -> CircularProgressIndicator(modifier = Modifier.size(32.dp))
            else -> {
                val amount = (couponPerformanceState as? Success)?.data
                    ?.formattedAmount ?: "-"
                Text(
                    text = amount,
                    style = MaterialTheme.typography.h5,
                    color = MaterialTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
