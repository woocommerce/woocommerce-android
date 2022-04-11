package com.woocommerce.android.ui.coupons.details

import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.coupons.components.CouponExpirationLabel
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
        state.couponSummary?.let { coupon ->
            CouponSummaryHeading(
                code = coupon.code,
                isActive = state.couponSummary.isActive
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
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        code?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.h5,
                color = MaterialTheme.colors.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
        CouponExpirationLabel(isActive)
    }
}

@Composable
fun CouponSummarySection(couponSummary: CouponSummaryUi) {
    Surface(
        elevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.coupon_summary_heading),
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SummaryLabel(couponSummary.discountType)
            SummaryLabel(couponSummary.summary)
            SummaryLabel(couponSummary.minimumSpending)
            SummaryLabel(couponSummary.maximumSpending)
            SummaryLabel(couponSummary.expiration)
        }
    }
}

@Composable
private fun SummaryLabel(text: String?) {
    text?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface,
        )
    }
}

@Composable
private fun CouponPerformanceSection(couponPerformanceState: CouponPerformanceState) {
    Surface(
        elevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.coupon_summary_performance_heading),
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
            text = stringResource(id = R.string.coupon_summary_performance_discounted_order_heading),
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = R.color.color_surface_variant)
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
            text = stringResource(id = R.string.coupon_summary_performance_amount_heading),
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = R.color.color_surface_variant)
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
