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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.woocommerce.android.ui.compose.component.DatePickerDialog
import com.woocommerce.android.ui.compose.component.ProgressDialog
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
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun EditCouponScreen(viewModel: EditCouponViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        EditCouponScreen(
            viewState = it,
            onAmountChanged = viewModel::onAmountChanged,
            onCouponCodeChanged = viewModel::onCouponCodeChanged,
            onRegenerateCodeClick = viewModel::onRegenerateCodeClick,
            onDescriptionButtonClick = viewModel::onDescriptionButtonClick,
            onExpiryDateChanged = viewModel::onExpiryDateChanged,
            onFreeShippingChanged = viewModel::onFreeShippingChanged,
            onUsageRestrictionsClick = viewModel::onUsageRestrictionsClick,
            onSelectProductsButtonClick = viewModel::onSelectProductsButtonClick,
            onSelectCategoriesButtonClick = viewModel::onSelectCategoriesButtonClick,
            onSaveClick = viewModel::onSaveClick
        )
    }
}

@Composable
fun EditCouponScreen(
    viewState: ViewState,
    onAmountChanged: (BigDecimal?) -> Unit = {},
    onCouponCodeChanged: (String) -> Unit = {},
    onRegenerateCodeClick: () -> Unit = {},
    onDescriptionButtonClick: () -> Unit = {},
    onExpiryDateChanged: (Date?) -> Unit = {},
    onFreeShippingChanged: (Boolean) -> Unit = {},
    onUsageRestrictionsClick: () -> Unit = {},
    onSelectProductsButtonClick: () -> Unit = {},
    onSelectCategoriesButtonClick: () -> Unit = {},
    onSaveClick: () -> Unit = {}
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
            onExpiryDateChanged = onExpiryDateChanged,
            onFreeShippingChanged = onFreeShippingChanged
        )
        ConditionsSection(viewState, onSelectProductsButtonClick, onSelectCategoriesButtonClick)
        UsageRestrictionsSection(viewState, onUsageRestrictionsClick)
        WCColoredButton(
            onClick = onSaveClick,
            text = stringResource(id = R.string.coupon_edit_save_button),
            modifier = Modifier
                .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                .fillMaxWidth(),
            enabled = viewState.hasChanges
        )
    }

    if (viewState.isSaving) {
        ProgressDialog(
            title = stringResource(id = R.string.coupon_edit_saving_dialog_title),
            subtitle = stringResource(id = R.string.coupon_edit_saving_dialog_subtitle)
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
    onExpiryDateChanged: (Date?) -> Unit,
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
        ExpiryField(viewState.couponDraft.dateExpires, onExpiryDateChanged)
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
private fun ConditionsSection(
    viewState: ViewState,
    onSelectProductsButtonClick: () -> Unit,
    onSelectCategoriesButtonClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        modifier = Modifier
            .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            .fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.coupon_edit_conditions_section).toUpperCase(Locale.current),
            style = MaterialTheme.typography.body2,
            color = colorResource(id = R.color.color_on_surface_medium)
        )
        WCOutlinedButton(
            onClick = onSelectProductsButtonClick,
            text = if (viewState.couponDraft.productIds.isEmpty()) {
                stringResource(R.string.coupon_conditions_products_all_products_title)
            } else {
                stringResource(
                    R.string.coupon_conditions_products_edit_products_title,
                    viewState.couponDraft.productIds.size
                )
            },
            leadingIcon = {
                if (viewState.couponDraft.productIds.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(dimensionResource(id = R.dimen.major_100))
                    )
                }
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.onSurface),
            modifier = Modifier.fillMaxWidth()
        )
        WCOutlinedButton(
            onClick = onSelectCategoriesButtonClick,
            text =
            if (viewState.couponDraft.categoryIds.isEmpty()) {
                stringResource(R.string.coupon_edit_select_categories_title)
            } else {
                stringResource(
                    R.string.coupon_edit_edit_products_title,
                    viewState.couponDraft.categoryIds.size
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = if (viewState.couponDraft.categoryIds.isEmpty())
                        Icons.Filled.Add
                    else
                        Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(id = R.dimen.major_100))
                )
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.onSurface),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
@Suppress("UnusedPrivateMember")
private fun UsageRestrictionsSection(
    viewState: ViewState,
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

@Suppress("LongMethod")
@Composable
private fun ExpiryField(dateExpires: Date?, onExpiryDateChanged: (Date?) -> Unit) {
    val dateFormat = remember { SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    WCOutlinedSpinner(
        onClick = { showDatePicker = true },
        value = dateExpires?.let { dateFormat.format(it) }
            ?: stringResource(id = R.string.coupon_edit_expiry_date_none),
        label = stringResource(id = R.string.coupon_edit_expiry_date),
        modifier = Modifier.fillMaxWidth()
    )

    if (showDatePicker) {
        DatePickerDialog(
            currentDate = dateExpires,
            onDateSelected = {
                showDatePicker = false
                onExpiryDateChanged(it)
            },
            onDismissRequest = { showDatePicker = false },
            neutralButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    onExpiryDateChanged(null)
                }) {
                    Text(stringResource(id = R.string.coupon_edit_expiry_clear_expiry_date))
                }
            },
            dateFormat = dateFormat
        )
    }
}

@Composable
@Preview
private fun EditCouponPreview() {
    WooTheme {
        EditCouponScreen(
            viewState = ViewState(
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
                hasChanges = true,
                isSaving = true
            )
        )
    }
}
