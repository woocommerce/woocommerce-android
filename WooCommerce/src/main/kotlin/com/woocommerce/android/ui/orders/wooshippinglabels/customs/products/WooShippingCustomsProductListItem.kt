package com.woocommerce.android.ui.orders.wooshippinglabels.customs.products

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.component.getText
import com.woocommerce.android.ui.compose.theme.LegacyWooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.components.RoundedBorderDropDownWithLabel
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.EuProductIdentifiers
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.InputValue

@Composable
fun WooShippingCustomsProductListItem(
    itemData: WooShippingCustomsProductUIModel,
    currencySymbol: String,
    weightUnit: String,
    onExpand: (isExtended: Boolean) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onTariffChanged: (String) -> Unit,
    showEuProductIdentifiers: Boolean,
    onEuProductIdentifiersChanged: (EuProductIdentifiers) -> Unit,
    onValuePerUnitChanged: (String) -> Unit,
    onWeightPerUnitChanged: (String) -> Unit,
    onCountrySelectorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotationAnimation = itemData.isExpanded
        .let { if (it) 180f else 0f }
        .let { animateFloatAsState(targetValue = it, label = "rotationAnimation") }

    val borderColor = when {
        itemData.isExpanded -> colorResource(R.color.color_on_surface)
        itemData.isValid(showEuProductIdentifiers).not() -> colorResource(R.color.color_error)
        else -> colorResource(R.color.divider_color)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .background(
                color = colorResource(R.color.color_surface),
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
            )
            .border(
                width = dimensionResource(R.dimen.minor_10),
                color = borderColor,
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
            )
            .clickable { onExpand(itemData.isExpanded.not()) }
            .padding(16.dp)
    ) {
        Row {
            Text(
                text = itemData.name,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.color_on_surface),
                modifier = modifier.weight(1f)
            )

            if (itemData.isValid(showEuProductIdentifiers).not()) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_error_filled_24dp),
                    tint = MaterialTheme.colors.error,
                    contentDescription = stringResource(
                        id = R.string.shipping_label_package_details_items_expand_content_description
                    )
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow_down),
                tint = MaterialTheme.colors.primary,
                contentDescription = stringResource(
                    id = R.string.shipping_label_package_details_items_expand_content_description
                ),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(dimensionResource(R.dimen.image_minor_60))
                    .rotate(rotationAnimation.value)
            )
        }

        if (itemData.isExpanded) {
            WooShippingCustomsProductExpandedListItem(
                itemData = itemData,
                currencySymbol = currencySymbol,
                weightUnit = weightUnit,
                onDescriptionChanged = onDescriptionChanged,
                onTariffChanged = onTariffChanged,
                showEuProductIdentifiers = showEuProductIdentifiers,
                onEuProductIdentifiersChanged = onEuProductIdentifiersChanged,
                onValuePerUnitChanged = onValuePerUnitChanged,
                onWeightPerUnitChanged = onWeightPerUnitChanged,
                onCountrySelectorClick = onCountrySelectorClick,
                modifier = modifier
            )
        } else {
            WooShippingCustomsProductCollapsedListItem(
                itemData = itemData,
                modifier = modifier
            )
        }
    }
}

@Composable
fun WooShippingCustomsProductCollapsedListItem(
    modifier: Modifier,
    itemData: WooShippingCustomsProductUIModel
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Spacer(modifier.padding(vertical = 4.dp))

        Row {
            Text(
                style = MaterialTheme.typography.body1,
                modifier = modifier.weight(1f),
                color = colorResource(id = R.color.color_on_surface_medium),
                text = itemData.description.currentInput
                    .takeIf { it.isNotBlank() }
                    ?: stringResource(id = R.string.woo_shipping_labels_customs_product_details_description_missing)
            )
            itemData.tariffNumber.currentInput
                .takeIf { it.isNotBlank() }?.let {
                    Text(
                        style = MaterialTheme.typography.body1,
                        color = colorResource(id = R.color.color_on_surface_medium),
                        text = it,
                    )
                }
        }
        Row {
            Text(
                style = MaterialTheme.typography.body1,
                modifier = modifier.weight(1f),
                color = colorResource(id = R.color.color_on_surface_medium),
                text = itemData.originCountry
                    .takeIf { it.isNotBlank() }
                    ?: stringResource(id = R.string.woo_shipping_labels_customs_product_details_origin_country_missing)
            )
            Text(
                text = itemData.formattedPriceAndWeight,
                color = colorResource(id = R.color.color_on_surface_medium),
                style = MaterialTheme.typography.body1,
            )
        }
    }
}

@Composable
fun WooShippingCustomsProductExpandedListItem(
    modifier: Modifier,
    itemData: WooShippingCustomsProductUIModel,
    currencySymbol: String,
    weightUnit: String,
    onDescriptionChanged: (String) -> Unit,
    onTariffChanged: (String) -> Unit,
    showEuProductIdentifiers: Boolean,
    onEuProductIdentifiersChanged: (EuProductIdentifiers) -> Unit,
    onValuePerUnitChanged: (String) -> Unit,
    onWeightPerUnitChanged: (String) -> Unit,
    onCountrySelectorClick: () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Column {
            DescriptionField(
                value = itemData.description,
                onValueChange = onDescriptionChanged,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            WCOutlinedTextField(
                value = itemData.tariffNumber.currentInput,
                onValueChange = onTariffChanged,
                label = stringResource(id = R.string.woo_shipping_labels_customs_product_details_tariff),
                singleLine = true,
                isError = itemData.tariffNumber is InputValue.Error,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth(),
                helperText = itemData.tariffNumber.errorMessageOrNull?.getText()
            )

            val uriHandler = LocalUriHandler.current
            WCTextButton(
                text = stringResource(id = R.string.woo_shipping_labels_customs_hs_tariff_info_button),
                onClick = { uriHandler.openUri(AppUrls.SHIPPING_LABEL_CUSTOMS_HS_TARIFF_NUMBER) },
                icon = ImageVector.vectorResource(R.drawable.ic_tintable_info_outline_24dp),
                allCaps = false,
                contentPadding = PaddingValues(
                    horizontal = 4.dp,
                    vertical = ButtonDefaults.TextButtonContentPadding.calculateTopPadding()
                )
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WCOutlinedTextField(
                    value = itemData.valuePerUnit.currentInput,
                    onValueChange = onValuePerUnitChanged,
                    label = stringResource(id = R.string.woo_shipping_labels_customs_product_details_value_per_unit),
                    trailingIcon = {
                        Text(
                            text = currencySymbol,
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = true,
                    isError = itemData.valuePerUnit is InputValue.Error,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.weight(1f),
                    helperText = itemData.valuePerUnit.errorMessageOrNull?.getText()
                )

                WCOutlinedTextField(
                    value = itemData.weightPerUnit.currentInput,
                    onValueChange = onWeightPerUnitChanged,
                    label = stringResource(id = R.string.woo_shipping_labels_customs_product_details_weight_per_unit),
                    trailingIcon = {
                        Text(
                            text = weightUnit,
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = true,
                    isError = itemData.weightPerUnit is InputValue.Error,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                    helperText = itemData.weightPerUnit.errorMessageOrNull?.getText()
                )
            }

            Spacer(Modifier.height(16.dp))

            OriginCountrySelector(
                value = itemData.originCountry,
                onClick = onCountrySelectorClick,
                modifier = Modifier.fillMaxWidth()
            )

            if (showEuProductIdentifiers) {
                val identifiers = itemData.euProductIdentifiers
                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.woo_shipping_labels_customs_eu_product_identifiers_title),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.SemiBold,
                    color = colorResource(R.color.color_on_surface)
                )

                Text(
                    text = stringResource(R.string.woo_shipping_labels_customs_eu_product_identifiers_description),
                    style = MaterialTheme.typography.body2,
                    color = colorResource(R.color.color_on_surface_medium)
                )

                Spacer(Modifier.height(16.dp))

                WCOutlinedTextField(
                    value = identifiers.sku,
                    onValueChange = { onEuProductIdentifiersChanged(identifiers.copy(sku = it)) },
                    label = stringResource(R.string.woo_shipping_labels_customs_product_details_sku),
                    singleLine = true,
                    isError = itemData.sku is InputValue.Error,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    helperText = itemData.sku.errorMessageOrNull?.getText()
                )

                Spacer(Modifier.height(16.dp))

                WCOutlinedTextField(
                    value = identifiers.manufacturerPartNumber,
                    onValueChange = {
                        onEuProductIdentifiersChanged(identifiers.copy(manufacturerPartNumber = it))
                    },
                    label = stringResource(
                        R.string.woo_shipping_labels_customs_product_details_manufacturer_part_number
                    ),
                    singleLine = true,
                    isError = itemData.manufacturerPartNumber is InputValue.Error,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    helperText = itemData.manufacturerPartNumber.errorMessageOrNull?.getText()
                )

                Spacer(Modifier.height(16.dp))

                WCOutlinedTextField(
                    value = identifiers.gtin,
                    onValueChange = { onEuProductIdentifiersChanged(identifiers.copy(gtin = it)) },
                    label = stringResource(R.string.woo_shipping_labels_customs_product_details_gtin),
                    singleLine = true,
                    isError = itemData.gtin is InputValue.Error,
                    enabled = !itemData.hasNoBarcode,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    helperText = itemData.gtin.errorMessageOrNull?.getText()
                )

                Row(
                    horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = identifiers.hasNoBarcode,
                            role = Role.Checkbox,
                            onValueChange = {
                                onEuProductIdentifiersChanged(identifiers.copy(hasNoBarcode = it))
                            }
                        )
                        .minimumInteractiveComponentSize()
                ) {
                    Text(
                        text = stringResource(R.string.woo_shipping_labels_customs_product_details_no_barcode),
                        color = colorResource(R.color.color_on_surface),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .weight(1f)
                    )
                    Checkbox(
                        checked = identifiers.hasNoBarcode,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            uncheckedColor = colorResource(R.color.color_on_surface_disabled)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DescriptionField(
    value: InputValue,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDialogOpen by rememberSaveable { mutableStateOf(false) }
    val localUriHandler = LocalUriHandler.current

    WCOutlinedTextField(
        value = value.currentInput,
        onValueChange = onValueChange,
        label = stringResource(id = R.string.woo_shipping_labels_customs_product_details_description),
        singleLine = true,
        isError = value is InputValue.Error,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        helperText = value.errorMessageOrNull?.getText(),
        trailingIcon = {
            IconButton(onClick = { isDialogOpen = true }) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_tintable_info_outline_24dp),
                    tint = MaterialTheme.colors.primary,
                    contentDescription = stringResource(R.string.woo_shipping_labels_customs_description_info_button)
                )
            }
        },
        modifier = modifier
    )

    if (isDialogOpen) {
        AlertDialog(
            title = {
                Text(text = stringResource(id = R.string.woo_shipping_labels_customs_product_details_description))
            },
            text = {
                Text(
                    text = stringResource(id = R.string.woo_shipping_labels_customs_description_info)
                )
            },
            confirmButton = {
                WCTextButton(
                    text = stringResource(id = R.string.learn_more),
                    onClick = {
                        isDialogOpen = false
                        localUriHandler.openUri(AppUrls.EU_SHIPPING_CUSTOMS_REQUIREMENTS)
                    }
                )
            },
            dismissButton = {
                WCTextButton(
                    text = stringResource(id = android.R.string.ok),
                    onClick = { isDialogOpen = false }
                )
            },
            onDismissRequest = { isDialogOpen = false }
        )
    }
}

@Composable
private fun OriginCountrySelector(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDialogOpen by rememberSaveable { mutableStateOf(false) }

    RoundedBorderDropDownWithLabel(
        label = stringResource(id = R.string.woo_shipping_labels_customs_product_details_origin_country),
        onClick = onClick,
        text = value
            .takeIf { it.isNotBlank() }
            ?: stringResource(R.string.woo_shipping_labels_customs_product_details_origin_country_selection),
        icon = {
            IconButton(
                onClick = { isDialogOpen = true }
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_tintable_info_outline_24dp),
                    tint = MaterialTheme.colors.primary,
                    contentDescription = stringResource(R.string.woo_shipping_labels_customs_origin_country_info_button)
                )
            }
        },
        modifier = modifier
    )

    if (isDialogOpen) {
        AlertDialog(
            title = {
                Text(text = stringResource(id = R.string.woo_shipping_labels_customs_product_details_origin_country))
            },
            text = {
                Text(
                    text = stringResource(id = R.string.woo_shipping_labels_customs_origin_country_info)
                )
            },
            confirmButton = {
                WCTextButton(
                    text = stringResource(id = android.R.string.ok),
                    onClick = { isDialogOpen = false }
                )
            },
            onDismissRequest = { isDialogOpen = false }
        )
    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun WooShippingCustomsProductListCollapsedItemPreview() {
    LegacyWooThemeWithBackground {
        Box(Modifier.padding(16.dp)) {
            WooShippingCustomsProductListItem(
                itemData = WooShippingCustomsProductUIModel(
                    productId = 0,
                    name = "Little Nap Brazil 250g",
                    description = InputValue.Data("Coffee Beans"),
                    tariffNumber = InputValue.Data("HS 14-1"),
                    valuePerUnit = InputValue.Data("$20.00"),
                    weightPerUnit = InputValue.Data("0.3kg"),
                    originCountry = "Japan",
                    originCountryCode = "JP",
                    quantity = 1F,
                    isExpanded = false,
                    formattedPriceAndWeight = "$15 • 5kg"
                ),
                currencySymbol = "$",
                weightUnit = "kg",
                onExpand = { },
                onDescriptionChanged = { },
                onTariffChanged = { },
                showEuProductIdentifiers = false,
                onEuProductIdentifiersChanged = { },
                onValuePerUnitChanged = { },
                onWeightPerUnitChanged = { },
                onCountrySelectorClick = { }
            )
        }
    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun WooShippingCustomsProductListCollapsedItemErrorPreview() {
    LegacyWooThemeWithBackground {
        Box(Modifier.padding(16.dp)) {
            WooShippingCustomsProductListItem(
                itemData = WooShippingCustomsProductUIModel(
                    productId = 0,
                    name = "Little Nap Brazil 250g",
                    description = InputValue.Error("Coffee Beans", 0),
                    tariffNumber = InputValue.Data("HS 14-1"),
                    valuePerUnit = InputValue.Data("$20.00"),
                    weightPerUnit = InputValue.Data("0.3kg"),
                    originCountry = "Japan",
                    originCountryCode = "JP",
                    quantity = 1F,
                    isExpanded = false,
                    formattedPriceAndWeight = "$15 • 5kg"
                ),
                currencySymbol = "$",
                weightUnit = "kg",
                onExpand = { },
                onDescriptionChanged = { },
                onTariffChanged = { },
                showEuProductIdentifiers = false,
                onEuProductIdentifiersChanged = { },
                onValuePerUnitChanged = { },
                onWeightPerUnitChanged = { },
                onCountrySelectorClick = { }
            )
        }
    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun WooShippingCustomsProductListExpandedItemPreview() {
    LegacyWooThemeWithBackground {
        Box(Modifier.padding(16.dp)) {
            WooShippingCustomsProductListItem(
                itemData = WooShippingCustomsProductUIModel(
                    productId = 0,
                    name = "Little Nap Brazil 250g",
                    description = InputValue.Data("Coffee Beans"),
                    tariffNumber = InputValue.Data("HS 14-1"),
                    valuePerUnit = InputValue.Data("20.00"),
                    weightPerUnit = InputValue.Data("0.3"),
                    originCountry = "Japan",
                    originCountryCode = "JP",
                    quantity = 1F,
                    isExpanded = true,
                    formattedPriceAndWeight = "$15 • 5kg"
                ),
                currencySymbol = "$",
                weightUnit = "kg",
                onExpand = { },
                onDescriptionChanged = { },
                onTariffChanged = { },
                showEuProductIdentifiers = false,
                onEuProductIdentifiersChanged = { },
                onValuePerUnitChanged = { },
                onWeightPerUnitChanged = { },
                onCountrySelectorClick = { }
            )
        }
    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun WooShippingCustomsProductListExpandedItemErrorPreview() {
    LegacyWooThemeWithBackground {
        Box(Modifier.padding(16.dp)) {
            WooShippingCustomsProductListItem(
                itemData = WooShippingCustomsProductUIModel(
                    productId = 0,
                    name = "Little Nap Brazil 250g",
                    description = InputValue.Error("Coffee Beans", UiString.UiStringText("Error")),
                    tariffNumber = InputValue.Error("HS 14-1", UiString.UiStringText("Error")),
                    sku = InputValue.Error(
                        "",
                        R.string.woo_shipping_labels_customs_product_details_eu_identifier_required
                    ),
                    manufacturerPartNumber = InputValue.Data("MPN-123"),
                    gtin = InputValue.Error("", R.string.woo_shipping_labels_customs_product_details_gtin_required),
                    hasNoBarcode = false,
                    valuePerUnit = InputValue.Data("20.00"),
                    weightPerUnit = InputValue.Data("0.3"),
                    originCountry = "Japan",
                    originCountryCode = "JP",
                    quantity = 1F,
                    isExpanded = true,
                    formattedPriceAndWeight = "$15 • 5kg"
                ),
                currencySymbol = "$",
                weightUnit = "kg",
                onExpand = { },
                onDescriptionChanged = { },
                onTariffChanged = { },
                showEuProductIdentifiers = true,
                onEuProductIdentifiersChanged = { },
                onValuePerUnitChanged = { },
                onWeightPerUnitChanged = { },
                onCountrySelectorClick = { }
            )
        }
    }
}
