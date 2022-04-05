package com.woocommerce.android.ui.coupons.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.coupons.details.CouponDetailsViewModel.CouponDetailsState
import com.woocommerce.android.ui.coupons.details.CouponDetailsViewModel.CouponUi

@Composable
fun CouponDetailsScreen(
    viewModel: CouponDetailsViewModel,
    onBackPress: () -> Boolean
) {
    val couponSummaryState by viewModel.couponState.observeAsState(CouponDetailsState())

    CouponDetailsScreen(
        couponSummaryState,
        onBackPress,
        viewModel::onCopyButtonClick
    )
}

@Composable
fun CouponDetailsScreen(
    state: CouponDetailsState,
    onBackPress: () -> Boolean,
    onCopyButtonClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        var showMenu by remember { mutableStateOf(false) }

        TopAppBar(
            backgroundColor = MaterialTheme.colors.surface,
            title = { Text(state.coupon?.code ?: "") },
            navigationIcon = {
                IconButton(onClick = { onBackPress() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Coupons Menu")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(onClick = {
                        onCopyButtonClick(state.coupon?.code ?: "")
                    }) {
                        Text(stringResource(id = R.string.coupon_details_menu_copy))
                    }
                    DropdownMenuItem(onClick = { /*TODO*/ }) {
                        Text(stringResource(id = R.string.coupon_details_menu_share))
                    }
                }
            }
        )

        state.coupon?.let { coupon ->
            CouponSummaryHeading(
                code = coupon.code,
                isActive = true
            )
            CouponSummarySection(coupon)
            CouponPerformanceSection()
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
                text = stringResource(id = R.string.coupon_details_heading),
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
fun CouponPerformanceSection() {
    Surface(
        elevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = stringResource(id = R.string.coupon_details_performance_heading),
                style = MaterialTheme.typography.h2,
                color = MaterialTheme.colors.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(id = R.string.coupon_details_performance_discounted_order_heading),
                        style = MaterialTheme.typography.h3,
                        color = colorResource(id = R.color.color_surface_variant),
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Hardcoded value for design purposes.
                    Text(
                        text = "12",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.onSurface,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(id = R.string.coupon_details_performance_amount_heading),
                        style = MaterialTheme.typography.h3,
                        color = colorResource(id = R.color.color_surface_variant),
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Hardcoded value for design purposes.
                    Text(
                        text = "$12.45",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.onSurface,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
