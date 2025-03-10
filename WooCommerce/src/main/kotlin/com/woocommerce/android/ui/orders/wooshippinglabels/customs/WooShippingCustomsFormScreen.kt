package com.woocommerce.android.ui.orders.wooshippinglabels.customs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedSpinner
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.ContentType
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.InputValue
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.RestrictionType
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.products.WooShippingCustomsProductListItem
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.products.WooShippingCustomsProductUIModel

@Composable
fun WooShippingCustomsFormScreen(viewModel: WooShippingCustomsFormViewModel) {
    val viewState by viewModel.viewState.observeAsState()
    WooShippingCustomsFormScreen(
        contentType = viewState?.contentType ?: ContentType.MERCHANDISE,
        restrictionType = viewState?.restrictionType ?: RestrictionType.NONE,
        itnValue = viewState?.itnValue ?: InputValue.Empty,
        otherContentDetailsInput = viewState?.otherContentInput ?: InputValue.Empty,
        otherRestrictionDetailsInput = viewState?.otherRestrictionInput ?: InputValue.Empty,
        returnToSenderChecked = viewState?.returnToSenderChecked ?: false,
        isAddCustomsButtonEnabled = viewState?.isAddCustomsButtonEnabled ?: false,
        shouldDisplayContentTypeInput = viewState?.shouldDisplayContentTypeInput ?: false,
        shouldDisplayRestrictionTypeInput = viewState?.shouldDisplayRestrictionTypeInput ?: false,
        shippingProducts = viewState?.shippingProducts ?: emptyList(),
        onContentTypeClick = viewModel::onContentTypeClick,
        onRestrictionTypeClick = viewModel::onRestrictionTypeClick,
        onItnChanged = viewModel::onITNChanged,
        onOtherContentDetailsInputChanged = viewModel::onOtherContentInputChanged,
        onOtherRestrictionDetailsInputChanged = viewModel::onRestrictionDetailsInputChanged,
        onReturnToSenderChanged = viewModel::onReturnToSenderChanged,
        onProductExpanded = viewModel::onShippableProductExpanded,
        onDescriptionChanged = viewModel::onShippableProductDescriptionChanged,
        onTariffChanged = viewModel::onShippableProductTariffNumberChanged,
        onValuePerUnitChanged = viewModel::onShippableProductValuePerUnitChanged,
        onWeightPerUnitChanged = viewModel::onShippableProductWeightPerUnitChanged,
        onCountrySelectorClick = viewModel::onCountrySelectorClick,
        onAddCustomsDataClick = viewModel::onAddCustomsDataClick
    )
}

@Composable
fun WooShippingCustomsFormScreen(
    modifier: Modifier = Modifier,
    contentType: ContentType,
    restrictionType: RestrictionType,
    itnValue: InputValue,
    otherContentDetailsInput: InputValue,
    otherRestrictionDetailsInput: InputValue,
    returnToSenderChecked: Boolean,
    isAddCustomsButtonEnabled: Boolean,
    shouldDisplayContentTypeInput: Boolean,
    shouldDisplayRestrictionTypeInput: Boolean,
    shippingProducts: List<WooShippingCustomsProductUIModel>,
    onContentTypeClick: () -> Unit,
    onRestrictionTypeClick: () -> Unit,
    onItnChanged: (String) -> Unit,
    onReturnToSenderChanged: (Boolean) -> Unit,
    onOtherContentDetailsInputChanged: (String) -> Unit,
    onOtherRestrictionDetailsInputChanged: (String) -> Unit,
    onProductExpanded: (position: Int, isExpanded: Boolean) -> Unit,
    onDescriptionChanged: (position: Int, description: String) -> Unit,
    onTariffChanged: (position: Int, tariff: String) -> Unit,
    onValuePerUnitChanged: (position: Int, valuePerUnit: String) -> Unit,
    onWeightPerUnitChanged: (position: Int, weightPerUnit: String) -> Unit,
    onCountrySelectorClick: (position: Int) -> Unit,
    onAddCustomsDataClick: () -> Unit

) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WCOutlinedSpinner(
                onClick = onContentTypeClick,
                value = stringResource(id = contentType.resourceId),
                label = stringResource(id = R.string.woo_shipping_labels_customs_content_type_label),
                modifier = modifier.fillMaxWidth()
            )

            AnimatedVisibility(shouldDisplayContentTypeInput) {
                WCOutlinedTextField(
                    value = otherContentDetailsInput.currentInput,
                    onValueChange = onOtherContentDetailsInputChanged,
                    label = stringResource(id = R.string.woo_shipping_labels_customs_content_details_label),
                    singleLine = true,
                    isError = otherContentDetailsInput is InputValue.Error,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = modifier.fillMaxWidth(),
                    helperText = otherContentDetailsInput.errorMessageOrNull
                        ?.let { stringResource(it) }
                        ?: stringResource(R.string.woo_shipping_labels_customs_content_details_description)
                )
            }

            WCOutlinedSpinner(
                onClick = onRestrictionTypeClick,
                value = stringResource(id = restrictionType.resourceId),
                label = stringResource(id = R.string.woo_shipping_labels_customs_restriction_type_label),
                modifier = modifier.fillMaxWidth()
            )

            AnimatedVisibility(shouldDisplayRestrictionTypeInput) {
                WCOutlinedTextField(
                    value = otherRestrictionDetailsInput.currentInput,
                    onValueChange = onOtherRestrictionDetailsInputChanged,
                    label = stringResource(id = R.string.woo_shipping_labels_customs_restriction_details_label),
                    singleLine = true,
                    isError = otherRestrictionDetailsInput is InputValue.Error,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = modifier.fillMaxWidth(),
                    helperText = otherRestrictionDetailsInput.errorMessageOrNull
                        ?.let { stringResource(it) }
                        ?: stringResource(R.string.woo_shipping_labels_customs_restriction_details_description)
                )
            }

            WCOutlinedTextField(
                value = itnValue.currentInput,
                onValueChange = onItnChanged,
                label = stringResource(id = R.string.woo_shipping_labels_customs_itn_label),
                singleLine = true,
                isError = itnValue is InputValue.Error,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = modifier.fillMaxWidth(),
                helperText = itnValue.errorMessageOrNull
                    ?.let { stringResource(it) }
            )

            Row(
                horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                modifier = modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.woo_shipping_labels_customs_return_to_sender_label),
                    modifier = modifier
                        .align(Alignment.CenterVertically)
                        .weight(1f)
                )
                Checkbox(
                    checked = returnToSenderChecked,
                    onCheckedChange = onReturnToSenderChanged,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Text(
                text = stringResource(R.string.woo_shipping_labels_customs_product_details_title),
                style = MaterialTheme.typography.titleMedium,
            )

            shippingProducts.forEachIndexed { index, product ->
                WooShippingCustomsProductListItem(
                    modifier = modifier.fillMaxWidth(),
                    itemData = product,
                    onExpand = { onProductExpanded(index, it) },
                    onDescriptionChanged = { onDescriptionChanged(index, it) },
                    onTariffChanged = { onTariffChanged(index, it) },
                    onValuePerUnitChanged = { onValuePerUnitChanged(index, it) },
                    onWeightPerUnitChanged = { onWeightPerUnitChanged(index, it) },
                    onCountrySelectorClick = { onCountrySelectorClick(index) }
                )
            }
        }
        WCColoredButton(
            modifier = modifier.fillMaxWidth(),
            enabled = isAddCustomsButtonEnabled,
            onClick = onAddCustomsDataClick,
            text = stringResource(id = R.string.woo_shipping_labels_customs_add_missing_information)
        )
    }
}

@Preview
@Composable
fun PreviewWooShippingCustomsFormScreen() {
    WooThemeWithBackground {
        WooShippingCustomsFormScreen(
            contentType = ContentType.MERCHANDISE,
            restrictionType = RestrictionType.NONE,
            itnValue = InputValue.Data("123456"),
            otherContentDetailsInput = InputValue.Data("Important Stuff"),
            otherRestrictionDetailsInput = InputValue.Data("Restricted Stuff"),
            returnToSenderChecked = false,
            isAddCustomsButtonEnabled = true,
            shouldDisplayContentTypeInput = true,
            shouldDisplayRestrictionTypeInput = false,
            shippingProducts = listOf(
                WooShippingCustomsProductUIModel(
                    productId = 0,
                    name = "Little Nap Brazil 250g",
                    description = InputValue.Data("Product Description"),
                    tariffNumber = InputValue.Data("123456"),
                    valuePerUnit = InputValue.Data("10.00"),
                    weightPerUnit = InputValue.Data("1.00"),
                    originCountry = "US",
                    quantity = 1F,
                    isExpanded = false
                ),
                WooShippingCustomsProductUIModel(
                    productId = 0,
                    name = "Little Nap Brazil 250g",
                    description = InputValue.Data("Product Description"),
                    tariffNumber = InputValue.Data("123456"),
                    valuePerUnit = InputValue.Data("10.00"),
                    weightPerUnit = InputValue.Data("1.00"),
                    originCountry = "US",
                    quantity = 1F,
                    isExpanded = true
                )
            ),
            onContentTypeClick = {},
            onRestrictionTypeClick = {},
            onItnChanged = {},
            onReturnToSenderChanged = {},
            onOtherContentDetailsInputChanged = {},
            onOtherRestrictionDetailsInputChanged = {},
            onProductExpanded = { _, _ -> },
            onAddCustomsDataClick = {},
            onDescriptionChanged = { _, _ -> },
            onTariffChanged = { _, _ -> },
            onValuePerUnitChanged = { _, _ -> },
            onWeightPerUnitChanged = { _, _ -> },
            onCountrySelectorClick = { }
        )
    }
}
