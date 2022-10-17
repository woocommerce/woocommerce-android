package com.woocommerce.android.ui.coupons.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.NullableBigDecimalTextFieldValueMapper
import com.woocommerce.android.ui.compose.component.NullableIntTextFieldValueMapper
import com.woocommerce.android.ui.compose.component.WCListItemWithInlineSubtitle
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTypedTextField
import com.woocommerce.android.ui.compose.component.WCSwitch
import com.woocommerce.android.ui.coupons.edit.CouponRestrictionsViewModel.ViewState
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
            onExcludeSaleItemsChanged = viewModel::onExcludeSaleItemsChanged,
            onAllowedEmailsButtonClicked = viewModel::onAllowedEmailsButtonClicked,
            onExcludeProductsButtonClick = viewModel::onExcludeProductsButtonClick,
            onExcludeCategoriesButtonClick = viewModel::onExcludeCategoriesButtonClick
        )
    }
}

@Composable
fun CouponRestrictionsScreen(
    viewState: CouponRestrictionsViewModel.ViewState,
    onMinimumAmountChanged: (BigDecimal?) -> Unit,
    onMaximumAmountChanged: (BigDecimal?) -> Unit,
    onUsageLimitPerCouponChanged: (Int?) -> Unit,
    onLimitUsageToXItemsChanged: (Int?) -> Unit,
    onUsageLimitPerUserChanged: (Int?) -> Unit,
    onIndividualUseChanged: (Boolean) -> Unit,
    onExcludeSaleItemsChanged: (Boolean) -> Unit,
    onAllowedEmailsButtonClicked: () -> Unit,
    onExcludeProductsButtonClick: () -> Unit,
    onExcludeCategoriesButtonClick: () -> Unit,
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
        SpendingRestrictionField(
            value = viewState.restrictions.minimumAmount,
            onValueChange = onMinimumAmountChanged,
            label = stringResource(id = R.string.coupon_restrictions_minimum_spend_hint, viewState.currencyCode),
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
        )

        SpendingRestrictionField(
            value = viewState.restrictions.maximumAmount,
            onValueChange = onMaximumAmountChanged,
            label = stringResource(id = R.string.coupon_restrictions_maximum_spend_hint, viewState.currencyCode),
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
        )

        WCOutlinedTypedTextField(
            value = viewState.restrictions.usageLimit,
            onValueChange = onUsageLimitPerCouponChanged,
            label = stringResource(id = R.string.coupon_restrictions_limit_per_coupon_hint),
            valueMapper = NullableIntTextFieldValueMapper(supportsNegativeValue = false),
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100)),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholderText = stringResource(id = R.string.coupon_restrictions_limit_per_coupon_placeholder)
        )

        if (viewState.showLimitUsageToXItems) {
            WCOutlinedTypedTextField(
                value = viewState.restrictions.limitUsageToXItems,
                onValueChange = onLimitUsageToXItemsChanged,
                label = stringResource(id = R.string.coupon_restrictions_amount_limit_hint),
                valueMapper = NullableIntTextFieldValueMapper(supportsNegativeValue = false),
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100)),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholderText = stringResource(id = R.string.coupon_restrictions_amount_limit_placeholder)
            )
        }

        WCOutlinedTypedTextField(
            value = viewState.restrictions.usageLimitPerUser,
            onValueChange = onUsageLimitPerUserChanged,
            label = stringResource(id = R.string.coupon_restrictions_limit_per_user_hint),
            valueMapper = NullableIntTextFieldValueMapper(supportsNegativeValue = false),
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100)),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholderText = stringResource(id = R.string.coupon_restrictions_limit_per_user_placeholder)
        )

        AllowedEmailsButton(
            allowedEmails = viewState.restrictions.restrictedEmails,
            onClick = onAllowedEmailsButtonClicked
        )

        IndividualUseSwitch(
            isForIndividualUse = viewState.restrictions.isForIndividualUse ?: false,
            onIndividualUseChanged = onIndividualUseChanged
        )
        SaleItemsSwitch(
            areSaleItemsExcluded = viewState.restrictions.areSaleItemsExcluded ?: false,
            onExcludeSaleItemsChanged = onExcludeSaleItemsChanged
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
        ExclusionsSection(viewState, onExcludeProductsButtonClick, onExcludeCategoriesButtonClick)
    }
}

@Composable
private fun SpendingRestrictionField(
    value: BigDecimal?,
    onValueChange: (BigDecimal?) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    WCOutlinedTypedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        valueMapper = NullableBigDecimalTextFieldValueMapper(supportsNegativeValue = false),
        modifier = modifier,
        // TODO use KeyboardType.Decimal after updating to Compose 1.2.0
        //  (https://issuetracker.google.com/issues/209835363)
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        placeholderText = stringResource(id = R.string.coupon_restrictions_minimum_maximum_spend_placeholder)
    )
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
private fun AllowedEmailsButton(allowedEmails: List<String>, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        val subtitle = if (allowedEmails.isEmpty()) {
            stringResource(id = R.string.coupon_restrictions_allowed_emails_placeholder)
        } else {
            allowedEmails.joinToString(", ")
        }

        WCListItemWithInlineSubtitle(
            text = stringResource(id = R.string.coupon_restrictions_allowed_emails),
            subtitle = subtitle,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(id = R.dimen.min_tap_target))
                .clickable(
                    enabled = true,
                    onClickLabel = stringResource(id = R.string.coupon_restrictions_allowed_emails),
                    role = Role.Button,
                    onClick = onClick
                ),
            showChevron = false,
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

@Composable
private fun ExclusionsSection(
    viewState: ViewState,
    onExcludeProductsButtonClick: () -> Unit,
    onExcludeCategoriesButtonClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
    ) {
        Text(
            text = stringResource(id = R.string.coupon_restrictions_exclusions_section_title)
                .toUpperCase(Locale.current),
            style = MaterialTheme.typography.body2,
            color = colorResource(id = R.color.color_on_surface_medium)
        )

        val productsButtonSuffix = if (viewState.restrictions.excludedProductIds.isEmpty()) ""
        else " (${viewState.restrictions.excludedProductIds.size})"
        WCOutlinedButton(
            onClick = onExcludeProductsButtonClick,
            text = "${stringResource(R.string.coupon_restrictions_exclude_products)}$productsButtonSuffix",
            leadingIcon = {
                Icon(
                    imageVector = if (viewState.restrictions.excludedProductIds.isEmpty()) Icons.Filled.Add
                    else Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(id = R.dimen.major_100))
                )
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.onSurface),
            modifier = Modifier.fillMaxWidth()
        )

        val categoriesButtonSuffix = if (viewState.restrictions.excludedCategoryIds.isEmpty()) ""
        else " (${viewState.restrictions.excludedCategoryIds.size})"
        WCOutlinedButton(
            onClick = onExcludeCategoriesButtonClick,
            text = "${stringResource(R.string.coupon_restrictions_exclude_categories)}$categoriesButtonSuffix",
            leadingIcon = {
                Icon(
                    imageVector = if (viewState.restrictions.excludedCategoryIds.isEmpty()) Icons.Filled.Add
                    else Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(id = R.dimen.major_100))
                )
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.onSurface),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
