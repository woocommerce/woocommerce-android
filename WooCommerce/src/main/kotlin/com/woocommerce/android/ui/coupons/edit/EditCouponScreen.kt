package com.woocommerce.android.ui.coupons.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.model.Coupon.Type
import com.woocommerce.android.model.Coupon.Type.Percent
import com.woocommerce.android.ui.compose.component.BigDecimalTextFieldValueMapper
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCOutlinedSpinner
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCOutlinedTypedTextField
import com.woocommerce.android.ui.compose.component.WCSwitch
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.ui.coupons.edit.EditCouponViewModel.ViewState
import java.math.BigDecimal

@Composable
fun EditCouponScreen(viewModel: EditCouponViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        EditCouponScreen(
            viewState = it,
            onAmountChanged = viewModel::onAmountChanged,
            onCouponCodeChanged = viewModel::onCouponCodeChanged,
            onRegenerateCodeClick = viewModel::onRegenerateCodeClick,
            onDescriptionButtonClick = viewModel::onDescriptionButtonClick,
            onFreeShippingChanged = viewModel::onFreeShippingChanged,
            onUsageRestrictionsClick = viewModel::onUsageRestrictionsClick
        )
    }
}

@Composable
fun EditCouponScreen(
    viewState: EditCouponViewModel.ViewState,
    onAmountChanged: (BigDecimal?) -> Unit = {},
    onCouponCodeChanged: (String) -> Unit = {},
    onRegenerateCodeClick: () -> Unit = {},
    onDescriptionButtonClick: () -> Unit = {},
    onFreeShippingChanged: (Boolean) -> Unit = {},
    onUsageRestrictionsClick: () -> Unit = {}
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
        DetailsSection(
            viewState = viewState,
            onAmountChanged = onAmountChanged,
            onCouponCodeChanged = onCouponCodeChanged,
            onRegenerateCodeClick = onRegenerateCodeClick,
            onDescriptionButtonClick = onDescriptionButtonClick,
            onFreeShippingChanged = onFreeShippingChanged
        )
        ConditionsSection(viewState)
        UsageRestrictionsSection(viewState, onUsageRestrictionsClick)
        WCColoredButton(
            onClick = { /*TODO*/ },
            text = stringResource(id = R.string.coupon_edit_save_button),
            modifier = Modifier
                .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                .fillMaxWidth(),
            enabled = viewState.hasChanges
        )
    }
}

@Composable
private fun DetailsSection(
    viewState: ViewState,
    onAmountChanged: (BigDecimal?) -> Unit,
    onCouponCodeChanged: (String) -> Unit,
    onRegenerateCodeClick: () -> Unit,
    onDescriptionButtonClick: () -> Unit,
    onFreeShippingChanged: (Boolean) -> Unit
) {
    val couponDraft = viewState.couponDraft
    val focusManager = LocalFocusManager.current
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        modifier = Modifier
            .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            .fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.coupon_edit_details_section).toUpperCase(Locale.current),
            style = MaterialTheme.typography.body2,
            color = colorResource(id = R.color.color_on_surface_medium)
        )
        AmountField(viewState.couponDraft.amount, viewState.amountUnit, viewState.couponDraft.type, onAmountChanged)
        // Coupon code field: display code uppercased, but always return it lowercased
        WCOutlinedTextField(
            value = couponDraft.code.orEmpty().toUpperCase(Locale.current),
            label = stringResource(id = R.string.coupon_edit_code_hint),
            onValueChange = { onCouponCodeChanged(it.toLowerCase(Locale.current)) },
            helperText = stringResource(id = R.string.coupon_edit_code_helper),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth()
        )
        WCTextButton(
            onClick = {
                focusManager.clearFocus()
                onRegenerateCodeClick()
            },
            text = stringResource(id = R.string.coupon_edit_regenerate_coupon)
        )
        DescriptionButton(viewState.couponDraft.description, onDescriptionButtonClick)
        WCOutlinedSpinner(
            onClick = { /*TODO*/ },
            value = couponDraft.dateExpires?.toString() ?: "None",
            label = stringResource(id = R.string.coupon_edit_expiry_date),
            modifier = Modifier.fillMaxWidth()
        )
        WCSwitch(
            text = stringResource(id = R.string.coupon_edit_free_shipping),
            checked = viewState.couponDraft.isShippingFree ?: false,
            onCheckedChange = onFreeShippingChanged,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
@Suppress("UnusedPrivateMember")
private fun ConditionsSection(viewState: EditCouponViewModel.ViewState) {
    /*TODO*/
}

@Composable
@Suppress("UnusedPrivateMember")
private fun UsageRestrictionsSection(
    viewState: EditCouponViewModel.ViewState,
    onUsageRestrictionsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.coupon_edit_usage_section).toUpperCase(Locale.current),
            style = MaterialTheme.typography.body2,
            color = colorResource(id = R.color.color_on_surface_medium),
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
        )

        TextButton(
            onClick = onUsageRestrictionsClick,
            contentPadding = PaddingValues(dimensionResource(id = R.dimen.major_100)),
            colors = ButtonDefaults.textButtonColors(
                contentColor = colorResource(id = R.color.color_on_surface)
            )
        ) {
            Column {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.coupon_edit_usage_restrictions),
                        style = MaterialTheme.typography.body1,
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_right),
                        contentDescription = null
                    )
                }
            }
        }

        Divider(
            color = colorResource(id = R.color.divider_color),
            thickness = dimensionResource(id = R.dimen.minor_10),
            modifier = Modifier.padding(start = dimensionResource(id = R.dimen.major_100))
        )
    }
}

@Composable
private fun AmountField(amount: BigDecimal?, amountUnit: String, type: Type?, onAmountChanged: (BigDecimal?) -> Unit) {
    WCOutlinedTypedTextField(
        value = amount ?: BigDecimal.ZERO,
        label = stringResource(id = R.string.coupon_edit_amount_hint, amountUnit),
        valueMapper = BigDecimalTextFieldValueMapper(supportsNegativeValue = true),
        onValueChange = onAmountChanged,
        helperText = stringResource(
            if (type is Percent) R.string.coupon_edit_amount_percentage_helper
            else R.string.coupon_edit_amount_rate_helper
        ),
        // TODO use KeyboardType.Decimal after updating to Compose 1.2.0
        //  (https://issuetracker.google.com/issues/209835363)
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DescriptionButton(description: String?, onButtonClicked: () -> Unit) {
    WCOutlinedButton(
        onClick = onButtonClicked,
        text = stringResource(
            id = if (description.isNullOrEmpty()) R.string.coupon_edit_add_description
            else R.string.coupon_edit_edit_description
        ),
        leadingIcon = {
            Icon(
                imageVector = if (description.isNullOrEmpty()) Icons.Filled.Add else Icons.Filled.Edit,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(id = R.dimen.major_100))
            )
        },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.onSurface),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
@Preview
private fun EditCouponPreview() {
    WooTheme {
        EditCouponScreen(
            viewState = EditCouponViewModel.ViewState(
                couponDraft = Coupon(
                    id = 0L,
                    code = "code",
                    amount = BigDecimal.TEN,
                    isShippingFree = true,
                    productIds = emptyList(),
                    categoryIds = emptyList(),
                    restrictions = Coupon.CouponRestrictions(
                        excludedProductIds = emptyList(),
                        excludedCategoryIds = emptyList(),
                        restrictedEmails = emptyList()
                    )
                ),
                localizedType = "Fixed Rate Discount",
                amountUnit = "%",
                hasChanges = true
            )
        )
    }
}
