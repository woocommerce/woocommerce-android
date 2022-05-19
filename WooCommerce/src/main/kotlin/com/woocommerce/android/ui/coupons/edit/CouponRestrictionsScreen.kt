package com.woocommerce.android.ui.coupons.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.BigDecimalTextFieldValueMapper
import com.woocommerce.android.ui.compose.component.WCOutlinedTypedTextField
import java.math.BigDecimal

@Composable
fun CouponRestrictionsScreen(viewModel: CouponRestrictionsViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        CouponRestrictionsScreen(it)
    }
}

@Composable
fun CouponRestrictionsScreen(viewState: CouponRestrictionsViewModel.ViewState) {
    val scrollState = rememberScrollState()
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        modifier = Modifier
            .background(color = MaterialTheme.colors.surface)
            .verticalScroll(scrollState)
            .padding(
                start = dimensionResource(id = R.dimen.major_100),
                top = dimensionResource(id = R.dimen.major_100),
                bottom = dimensionResource(id = R.dimen.major_100)
            )
            .fillMaxSize()
    ) {
        WCOutlinedTypedTextField(
            value = viewState.restrictions.minimumAmount ?: BigDecimal.ZERO,
            onValueChange = { },
            label = stringResource(id = R.string.coupon_restrictions_minimum_spend_hint, viewState.currencyCode),
            valueMapper = BigDecimalTextFieldValueMapper(supportsNegativeValue = false),
            modifier = Modifier.padding(end = dimensionResource(id = R.dimen.major_100))
        )

        WCOutlinedTypedTextField(
            value = viewState.restrictions.maximumAmount ?: BigDecimal.ZERO,
            onValueChange = { },
            label = stringResource(id = R.string.coupon_restrictions_maximum_spend_hint, viewState.currencyCode),
            valueMapper = BigDecimalTextFieldValueMapper(supportsNegativeValue = false),
            modifier = Modifier.padding(end = dimensionResource(id = R.dimen.major_100))
        )
    }
}
