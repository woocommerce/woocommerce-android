package com.woocommerce.android.ui.coupons.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.extensions.orNullIfEmpty
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCOverflowMenuItem
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.coupons.components.CouponExpirationLabel
import com.woocommerce.android.ui.coupons.details.CouponDetailsViewModel.CouponDetailsState
import com.woocommerce.android.ui.coupons.details.CouponDetailsViewModel.CouponPerformanceState
import com.woocommerce.android.ui.coupons.details.CouponDetailsViewModel.CouponPerformanceState.Loading
import com.woocommerce.android.ui.coupons.details.CouponDetailsViewModel.CouponPerformanceState.Success
import com.woocommerce.android.ui.coupons.details.CouponDetailsViewModel.CouponSummaryUi

@Composable
fun CouponDetailsScreen(
    viewModel: CouponDetailsViewModel,
    onBackPress: () -> Unit
) {
    val couponSummaryState by viewModel.couponState.observeAsState(CouponDetailsState())

    CouponDetailsScreen(
        couponSummaryState,
        onBackPress,
        viewModel::onCopyButtonClick,
        viewModel::onShareButtonClick,
        viewModel::onEditButtonClick,
        viewModel::onDeleteButtonClick,
    )
}

@Composable
fun CouponDetailsScreen(
    state: CouponDetailsState,
    onBackPress: () -> Unit,
    onCopyButtonClick: () -> Unit,
    onShareButtonClick: () -> Unit,
    onEditButtonClick: () -> Unit,
    onDeleteButtonClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        var showDeleteDialog by remember { mutableStateOf(false) }

        Toolbar(
            title = state.couponSummary?.code ?: "",
            onNavigationButtonClick = onBackPress,
            actions = {
                OverflowAction(contentDescription = stringResource(id = R.string.more_menu)) { dismiss ->
                    WCOverflowMenuItem(
                        text = stringResource(id = R.string.coupon_details_menu_copy),
                        onClick = {
                            dismiss()
                            onCopyButtonClick()
                        }
                    )
                    WCOverflowMenuItem(
                        text = stringResource(id = R.string.coupon_details_menu_share),
                        onClick = {
                            dismiss()
                            onShareButtonClick()
                        }
                    )

                    if (state.couponSummary?.isEditable == true) {
                        WCOverflowMenuItem(
                            text = stringResource(id = R.string.coupon_details_menu_edit),
                            onClick = {
                                dismiss()
                                onEditButtonClick()
                            }
                        )

                        WCOverflowMenuItem(
                            text = stringResource(id = R.string.coupon_details_delete),
                            onClick = {
                                dismiss()
                                showDeleteDialog = true
                            },
                            isDestructive = true
                        )
                    }
                }
            }
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            state.couponSummary?.let { coupon ->
                CouponSummaryHeading(
                    code = coupon.code,
                    isActive = state.couponSummary.isActive,
                    description = coupon.description
                )
                CouponSummarySection(coupon)
            }
            state.couponPerformanceState?.let {
                CouponPerformanceSection(it)
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = {
                    Text(stringResource(id = R.string.coupon_details_delete))
                },
                text = {
                    Text(stringResource(id = R.string.coupon_details_delete_confirmation))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            onDeleteButtonClick()
                        }
                    ) {
                        Text(
                            stringResource(id = R.string.delete).uppercase(),
                            color = MaterialTheme.colors.secondary
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false }
                    ) {
                        Text(
                            stringResource(id = R.string.cancel).uppercase(),
                            color = MaterialTheme.colors.secondary
                        )
                    }
                }
            )
        }
    }
}

@Preview
@Composable
fun CouponDetailsScreenPreview() {
    WooThemeWithBackground {
        CouponDetailsScreen(
            state = CouponDetailsState(
                couponSummary = CouponSummaryUi(
                    code = "COUPON_CODE",
                    isEditable = true,
                    isActive = true,
                    description = "This is a coupon description.",
                    summary = "10% off entire order",
                    isForIndividualUse = true,
                    isShippingFree = false,
                    areSaleItemsExcluded = true,
                    discountType = "Percentage discount",
                    minimumSpending = "Minimum spend: $100.00",
                    maximumSpending = null,
                    usageLimitPerCoupon = "Usage limit: 100",
                    usageLimitPerUser = "Usage limit per user: 1",
                    usageLimitPerItems = null,
                    expiration = "Expires on December 31, 2024",
                    emailRestrictions = "Restricted to: user@example.com"
                ),
                couponPerformanceState = Success(
                    CouponDetailsViewModel.CouponPerformanceUi(
                        ordersCount = 50,
                        formattedAmount = "$500.00"
                    )
                )
            ),
            onBackPress = {},
            onCopyButtonClick = {},
            onShareButtonClick = {},
            onEditButtonClick = {},
            onDeleteButtonClick = {}
        )
    }
}

@Composable
fun CouponSummaryHeading(
    code: String?,
    isActive: Boolean,
    description: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(id = R.dimen.major_100))
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
        description?.orNullIfEmpty()?.let {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_150)))
            Text(
                text = it,
                style = MaterialTheme.typography.caption,
                color = colorResource(id = R.color.color_on_surface_medium),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CouponSummarySection(couponSummary: CouponSummaryUi) {
    Surface(
        elevation = dimensionResource(id = R.dimen.minor_10),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
            modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))
        ) {
            Text(
                text = stringResource(id = R.string.coupon_details_heading),
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.minor_100))
            )
            SummaryLabel(couponSummary.discountType)
            SummaryLabel(couponSummary.summary)
            if (couponSummary.isForIndividualUse) {
                SummaryLabel(stringResource(id = R.string.coupon_details_individual_use_only))
            }
            if (couponSummary.isShippingFree) {
                SummaryLabel(stringResource(id = R.string.coupon_details_allows_free_shipping))
            }
            if (couponSummary.areSaleItemsExcluded) {
                SummaryLabel(stringResource(id = R.string.coupon_details_excludes_sale_items))
            }
            SummaryLabel(couponSummary.minimumSpending)
            SummaryLabel(couponSummary.maximumSpending)
            SummaryLabel(couponSummary.usageLimitPerCoupon)
            SummaryLabel(couponSummary.usageLimitPerUser)
            SummaryLabel(couponSummary.usageLimitPerItems)
            SummaryLabel(couponSummary.expiration)
            SummaryLabel(couponSummary.emailRestrictions)
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
        elevation = dimensionResource(id = R.dimen.minor_10),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))
        ) {
            Text(
                text = stringResource(id = R.string.coupon_details_performance_heading),
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))

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
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
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
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        modifier = modifier
    ) {
        Text(
            text = stringResource(id = R.string.coupon_details_performance_amount_heading),
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = R.color.color_surface_variant)
        )
        when (couponPerformanceState) {
            is Loading -> CircularProgressIndicator(modifier = Modifier.size(dimensionResource(id = R.dimen.major_200)))
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
