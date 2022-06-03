package com.woocommerce.android.ui.coupons.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.BigDecimalTextFieldValueMapper
import com.woocommerce.android.ui.compose.component.IntTextFieldValueMapper
import com.woocommerce.android.ui.compose.component.WCFullWidthTextButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTypedTextField
import com.woocommerce.android.ui.compose.component.WCSwitch
import java.math.BigDecimal

@Composable
fun CouponRestrictionsScreen(viewModel: CouponRestrictionsViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        CouponRestrictionsScreen(
            viewState = it,
            onMinimumAmountChanged = viewModel::onMinimumAmountChanged,
            onMaximumAmountChanged = viewModel::onMaximumAmountChanged,
            onUsageLimitPerCouponChanged = viewModel::onUsageLimitPerCouponChanged,
            onLimitUsageToXItemsChanged = viewModel::onLimitUsageToXItemsChanged,
            onUsageLimitPerUserChanged = viewModel::onUsageLimitPerUserChanged,
            onIndividualUseChanged = viewModel::onIndividualUseChanged,
            onExcludeSaleItemsChanged = viewModel::onExcludeSaleItemsChanged
        )
    }
}

@Composable
fun CouponRestrictionsScreen(
    viewState: CouponRestrictionsViewModel.ViewState,
    onMinimumAmountChanged: (BigDecimal) -> Unit,
    onMaximumAmountChanged: (BigDecimal) -> Unit,
    onUsageLimitPerCouponChanged: (Int) -> Unit,
    onLimitUsageToXItemsChanged: (Int) -> Unit,
    onUsageLimitPerUserChanged: (Int) -> Unit,
    onIndividualUseChanged: (Boolean) -> Unit,
    onExcludeSaleItemsChanged: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        modifier = Modifier
            .background(color = MaterialTheme.colors.surface)
            .verticalScroll(scrollState)
            .padding(vertical = dimensionResource(id = R.dimen.major_100))
            .fillMaxSize()
    ) {
        WCOutlinedTypedTextField(
            value = viewState.restrictions.minimumAmount ?: BigDecimal.ZERO,
            onValueChange = onMinimumAmountChanged,
            label = stringResource(id = R.string.coupon_restrictions_minimum_spend_hint, viewState.currencyCode),
            valueMapper = BigDecimalTextFieldValueMapper(supportsNegativeValue = false),
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100)),
            // TODO use KeyboardType.Decimal after updating to Compose 1.2.0
            //  (https://issuetracker.google.com/issues/209835363)
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)

        )

        WCOutlinedTypedTextField(
            value = viewState.restrictions.maximumAmount ?: BigDecimal.ZERO,
            onValueChange = onMaximumAmountChanged,
            label = stringResource(id = R.string.coupon_restrictions_maximum_spend_hint, viewState.currencyCode),
            valueMapper = BigDecimalTextFieldValueMapper(supportsNegativeValue = false),
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100)),
            // TODO use KeyboardType.Decimal after updating to Compose 1.2.0
            //  (https://issuetracker.google.com/issues/209835363)
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        WCOutlinedTypedTextField(
            value = viewState.restrictions.usageLimit ?: 0,
            onValueChange = onUsageLimitPerCouponChanged,
            label = stringResource(id = R.string.coupon_restrictions_limit_per_coupon_hint),
            valueMapper = IntTextFieldValueMapper(supportsNegativeValue = false),
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100)),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        if (viewState.showLimitUsageToXItems) {
            WCOutlinedTypedTextField(
                value = viewState.restrictions.limitUsageToXItems ?: 0,
                onValueChange = onLimitUsageToXItemsChanged,
                label = stringResource(id = R.string.coupon_restrictions_amount_limit_hint),
                valueMapper = IntTextFieldValueMapper(supportsNegativeValue = false),
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100)),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        WCOutlinedTypedTextField(
            value = viewState.restrictions.usageLimitPerUser ?: 0,
            onValueChange = onUsageLimitPerUserChanged,
            label = stringResource(id = R.string.coupon_restrictions_limit_per_user_hint),
            valueMapper = IntTextFieldValueMapper(supportsNegativeValue = false),
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100)),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        AllowedEmailsButton()

        IndividualUseSwitch(
            isForIndividualUse = viewState.restrictions.isForIndividualUse ?: false,
            onIndividualUseChanged = onIndividualUseChanged
        )
        SaleItemsSwitch(
            areSaleItemsExcluded = viewState.restrictions.areSaleItemsExcluded ?: false,
            onExcludeSaleItemsChanged = onExcludeSaleItemsChanged
        )
    }
}

@Composable
private fun IndividualUseSwitch(isForIndividualUse: Boolean, onIndividualUseChanged: (Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        WCSwitch(
            text = stringResource(id = R.string.coupon_restrictions_individual_use),
            checked = isForIndividualUse,
            onCheckedChange = onIndividualUseChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.major_100))
        )
        Divider(
            modifier = Modifier.padding(
                bottom = dimensionResource(id = R.dimen.major_100),
                start = dimensionResource(id = R.dimen.major_100)
            )
        )
        Text(
            text = stringResource(id = R.string.coupon_restrictions_individual_use_hint),
            style = MaterialTheme.typography.caption,
            color = colorResource(id = R.color.color_on_surface_medium),
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
        )
    }
}

@Composable
private fun AllowedEmailsButton() {
    Column(Modifier.fillMaxWidth()) {
        WCFullWidthTextButton(
            onClick = { /* TODO */ },
            text = stringResource(id = R.string.coupon_restrictions_allowed_emails),
            inlineText = stringResource(id = R.string.coupon_restrictions_allowed_emails_placeholder),
            showChevron = false
        )
        Divider(
            modifier = Modifier.padding(
                bottom = dimensionResource(id = R.dimen.major_100),
                start = dimensionResource(id = R.dimen.major_100)
            )
        )
    }
}

@Composable
private fun SaleItemsSwitch(areSaleItemsExcluded: Boolean, onExcludeSaleItemsChanged: (Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        WCSwitch(
            text = stringResource(id = R.string.coupon_restrictions_exclude_sale_items),
            checked = areSaleItemsExcluded,
            onCheckedChange = onExcludeSaleItemsChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.major_100))
        )
        Divider(
            modifier = Modifier.padding(
                bottom = dimensionResource(id = R.dimen.major_100),
                start = dimensionResource(id = R.dimen.major_100)
            )
        )
        Text(
            text = stringResource(id = R.string.coupon_restrictions_exclude_sale_items_hint),
            style = MaterialTheme.typography.caption,
            color = colorResource(id = R.color.color_on_surface_medium),
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
        )
    }
}
