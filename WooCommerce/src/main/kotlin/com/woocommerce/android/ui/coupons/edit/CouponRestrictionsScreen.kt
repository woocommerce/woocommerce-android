package com.woocommerce.android.ui.coupons.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.BigDecimalTextFieldValueMapper
import com.woocommerce.android.ui.compose.component.WCOutlinedTypedTextField
import com.woocommerce.android.ui.compose.component.WCSwitch
import java.math.BigDecimal

@Composable
fun CouponRestrictionsScreen(viewModel: CouponRestrictionsViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        CouponRestrictionsScreen(
            viewState = it,
            onIndividualUseChanged = viewModel::onIndividualUseChanged,
            onExcludeSaleItemsChanged = viewModel::onExcludeSaleItemsChanged
        )
    }
}

@Composable
fun CouponRestrictionsScreen(
    viewState: CouponRestrictionsViewModel.ViewState,
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
            onValueChange = { },
            label = stringResource(id = R.string.coupon_restrictions_minimum_spend_hint, viewState.currencyCode),
            valueMapper = BigDecimalTextFieldValueMapper(supportsNegativeValue = false),
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
        )

        WCOutlinedTypedTextField(
            value = viewState.restrictions.maximumAmount ?: BigDecimal.ZERO,
            onValueChange = { },
            label = stringResource(id = R.string.coupon_restrictions_maximum_spend_hint, viewState.currencyCode),
            valueMapper = BigDecimalTextFieldValueMapper(supportsNegativeValue = false),
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
        )

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

