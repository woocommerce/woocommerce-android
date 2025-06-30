package com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCOutlinedSpinner
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel

@Composable
fun WooShippingCustomPackageCreationScreen(viewModel: WooShippingLabelPackageCreationViewModel) {
    val viewState by viewModel.viewState.observeAsState()
    val packageType = viewState?.customPackageCreationData?.type?.resourceId
        ?.let { stringResource(id = it) }.orEmpty()

    WooShippingCustomPackageCreationScreen(
        packageType = packageType,
        packageName = viewState?.customPackageCreationData?.name.orEmpty(),
        packageHeight = viewState?.customPackageCreationData?.height.orEmpty(),
        packageLength = viewState?.customPackageCreationData?.length.orEmpty(),
        packageWidth = viewState?.customPackageCreationData?.width.orEmpty(),
        packageWeight = viewState?.customPackageCreationData?.weight.orEmpty(),
        isAddPackageEnabled = viewState?.customPackageCreationData?.isValid ?: false,
        isSaveAsTemplateChecked = viewState?.customPackageCreationData?.saveAsTemplate ?: false,
        onAddPackageClick = viewModel::onAddCustomPackageClick,
        onPackageTypeClick = viewModel::onPackageTypeSpinnerClick,
        onLengthChange = viewModel::onLengthChange,
        onWidthChange = viewModel::onWidthChange,
        onHeightChange = viewModel::onHeightChange,
        onWeightChange = viewModel::onWeightChange,
        onPackageNameChange = viewModel::onPackageNameChange,
        onSavePackageChanged = viewModel::onSavePackageChanged
    )
}

@Composable
fun WooShippingCustomPackageCreationScreen(
    modifier: Modifier = Modifier,
    packageName: String,
    packageType: String,
    packageLength: String,
    packageWidth: String,
    packageHeight: String,
    packageWeight: String,
    isAddPackageEnabled: Boolean,
    isSaveAsTemplateChecked: Boolean,
    onAddPackageClick: (saveAsTemplate: Boolean) -> Unit,
    onPackageTypeClick: () -> Unit,
    onLengthChange: (String) -> Unit,
    onWidthChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onPackageNameChange: (String) -> Unit,
    onSavePackageChanged: (Boolean) -> Unit
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
            Column(modifier = modifier) {
                WCOutlinedSpinner(
                    onClick = onPackageTypeClick,
                    value = packageType,
                    label = stringResource(id = R.string.woo_shipping_labels_package_creation_package_type),
                    modifier = modifier.fillMaxWidth()
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = modifier.fillMaxWidth()
            ) {
                WCOutlinedTextField(
                    value = packageLength,
                    onValueChange = onLengthChange,
                    label = stringResource(id = R.string.woo_shipping_labels_package_creation_length),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = modifier.weight(1f)
                )

                WCOutlinedTextField(
                    value = packageWidth,
                    onValueChange = onWidthChange,
                    label = stringResource(id = R.string.woo_shipping_labels_package_creation_width),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = modifier.weight(1f)
                )

                WCOutlinedTextField(
                    value = packageHeight,
                    onValueChange = onHeightChange,
                    label = stringResource(id = R.string.woo_shipping_labels_package_creation_height),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = modifier.weight(1f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                modifier = modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.woo_shipping_labels_package_creation_save_package_option),
                    modifier = modifier.align(Alignment.CenterVertically)
                )
                Checkbox(
                    checked = isSaveAsTemplateChecked,
                    onCheckedChange = onSavePackageChanged,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colors.primary,
                        uncheckedColor = MaterialTheme.colors.onSurface
                    ),
                )
            }
            if (isSaveAsTemplateChecked) {
                Column(modifier = modifier) {
                    WCOutlinedTextField(
                        value = packageWeight,
                        onValueChange = onWeightChange,
                        label = stringResource(id = R.string.woo_shipping_labels_package_creation_weight),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = modifier.fillMaxWidth()
                    )
                }
                Column(modifier = modifier) {
                    WCOutlinedTextField(
                        value = packageName,
                        onValueChange = onPackageNameChange,
                        label = stringResource(id = R.string.woo_shipping_labels_package_creation_package_name),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = modifier.fillMaxWidth()
                    )
                }
            }
        }
        Button(
            modifier = modifier.fillMaxWidth(),
            enabled = isAddPackageEnabled,
            onClick = { onAddPackageClick(isSaveAsTemplateChecked) }
        ) {
            Text(stringResource(id = R.string.woo_shipping_labels_package_creation_add_package))
        }
    }
}

@Preview
@Composable
fun PreviewWooShippingCustomPackageCreationScreen() {
    WooThemeWithBackground {
        WooShippingCustomPackageCreationScreen(
            packageName = "Custom Package",
            packageType = "Box",
            packageLength = "10",
            packageWidth = "10",
            packageHeight = "10",
            packageWeight = "10",
            isAddPackageEnabled = true,
            isSaveAsTemplateChecked = true,
            onAddPackageClick = {},
            onPackageTypeClick = {},
            onLengthChange = {},
            onWidthChange = {},
            onHeightChange = {},
            onWeightChange = {},
            onPackageNameChange = {},
            onSavePackageChanged = {}
        )
    }
}
