package com.woocommerce.android.ui.coupons.details

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
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
fun CouponDetailsScreen(
    viewModel: CouponDetailsViewModel,
    onBackPress: () -> Boolean
) {
    val couponSummaryState by viewModel.couponState.observeAsState(CouponDetailsState())

    CouponDetailsScreen(
        couponSummaryState,
        onBackPress,
        viewModel::onCopyButtonClick,
        viewModel::onShareButtonClick
    )
}

@Composable
@Suppress("LongMethod")
fun CouponDetailsScreen(
    state: CouponDetailsState,
    onBackPress: () -> Boolean,
    onCopyButtonClick: () -> Unit,
    onShareButtonClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        var showMenu by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }

        TopAppBar(
            backgroundColor = MaterialTheme.colors.surface,
            title = { Text(state.couponSummary?.code ?: "") },
            navigationIcon = {
                IconButton(onClick = { onBackPress() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Coupons Menu",
                        tint = colorResource(id = R.color.action_menu_fg_selector)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(onClick = onCopyButtonClick) {
                        Text(stringResource(id = R.string.coupon_details_menu_copy))
                    }
                    DropdownMenuItem(onClick = onShareButtonClick) {
                        Text(stringResource(id = R.string.coupon_details_menu_share))
                    }
                    DropdownMenuItem(onClick = { showDeleteDialog = true }) {
                        Text(
                            stringResource(id = R.string.coupon_details_delete),
                            color = colorResource(id = R.color.woo_red_30)
                        )
                    }
                }
            }
        )

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

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                text = {
                    Text(stringResource(id = R.string.coupon_details_delete_confirmation))
                },
                confirmButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false }
                    ) {
                        Text(
                            stringResource(id = R.string.coupon_details_delete).uppercase(),
                            color = colorResource(id = R.color.woo_red_30)
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false }
                    ) {
                        Text(
                            stringResource(id = R.string.cancel).uppercase(),
                            color = colorResource(id = R.color.woo_red_30)
                        )
                    }
                }
            )
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
                text = stringResource(id = R.string.coupon_details_heading),
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
                text = stringResource(id = R.string.coupon_details_performance_heading),
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
            text = stringResource(id = R.string.coupon_details_performance_discounted_order_heading),
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
            text = stringResource(id = R.string.coupon_details_performance_amount_heading),
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
