package com.woocommerce.android.ui.coupons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.woocommerce.android.ui.coupons.CouponSummaryViewModel.CouponSummaryState

@Composable
fun CouponSummaryScreen(viewModel: CouponSummaryViewModel) {
    val couponSummaryState by viewModel.couponState.observeAsState(CouponSummaryState())

    CouponSummaryScreen(state = couponSummaryState)
}

@Composable
fun CouponSummaryScreen(state: CouponSummaryState) {
    state.coupon?.let { coupon ->
        CouponSummaryHeading(
            code = coupon.code,
            isActive = true
        )
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
                style = MaterialTheme.typography.h2,
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
