package com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.LeadingIconTab
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.SelectionCheck
import com.woocommerce.android.ui.compose.modifiers.dashedBorder
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.RoundedCornerBoxWithBorder
import com.woocommerce.android.ui.orders.wooshippinglabels.ShippingLabelSampleData
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.WooShippingCarrier
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Composable
internal fun ShippingRatesCard(
    state: WooShippingLabelCreationViewModel.ShippingRatesState.DataState,
    onSelectedRateSortOrderChanged: (ShippingSortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ShippingRatesHeader(
            selectedSortOption = state.selectedRatesSortOrder,
            onSortOptionSelected = onSelectedRateSortOrderChanged,
            modifier = Modifier.padding(start = dimensionResource(R.dimen.major_100))
        )
        ShippingRates(
            shippingRates = state.shippingRates,
            selectedRate = state.selectedRate,
            onSelectedSippingRateChanged = state.onSelectedShippingRateChanged,
            onSelectedRateOptionChanged = state.onSelectedRateOptionChanged,
        )
    }
}

@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.PIXEL)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.PIXEL)
@Composable
private fun ShippingRatesCardPreview() {
    val rates = ShippingLabelSampleData.generateShippingRates()
    val selected = rates.values.first().first()
    WooThemeWithBackground {
        ShippingRatesCard(
            state = ShippingLabelSampleData.getShippingRatesSection(),
            onSelectedRateSortOrderChanged = {},
        )
    }
}

@Composable
internal fun ShippingRatesSectionMissingInfo(
    missingInfo: WooShippingLabelCreationViewModel.ShippingRatesState.MissingInfo,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .dashedBorder(
                color = colorResource(R.color.divider_color),
                strokeWidth = 1.dp,
                dashLength = 4.dp,
                gapLength = 6.dp,
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
            )
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier.size(88.dp),
            painter = painterResource(id = R.drawable.ic_themes),
            contentDescription = stringResource(id = R.string.woopos_error_icon_content_description),
        )
        Text(
            text = stringResource(missingInfo.missingTitle),
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_200))
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = stringResource(missingInfo.missingDescription),
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = R.color.color_on_surface_medium),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun ShippingRatesLoading(
    selectedSortOption: ShippingSortOption,
    onSelectedRateSortOrderChanged: (ShippingSortOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ShippingRatesHeader(
            selectedSortOption = selectedSortOption,
            onSortOptionSelected = onSelectedRateSortOrderChanged,
            modifier = Modifier.padding(start = dimensionResource(R.dimen.major_100))
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .dashedBorder(
                    color = colorResource(R.color.divider_color),
                    strokeWidth = 1.dp,
                    dashLength = 4.dp,
                    gapLength = 6.dp,
                    shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
                )
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                modifier = Modifier.size(88.dp),
                painter = painterResource(id = R.drawable.ic_themes),
                contentDescription = null,
            )
            CircularProgressIndicator(modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_200)))
        }
    }
}

@Composable
private fun ShippingRatesHeader(
    selectedSortOption: ShippingSortOption,
    onSortOptionSelected: (ShippingSortOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.shipping_label_shipping_service_title),
            color = MaterialTheme.colors.onSurface,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        SortingDropdownMenu(
            selectedSortOption = selectedSortOption,
            onSortOptionSelected = onSortOptionSelected,
            modifier = Modifier.padding(dimensionResource(R.dimen.major_100))
        )
    }
}

@Preview
@Composable
private fun ShippingRatesHeaderPreview() {
    WooThemeWithBackground {
        ShippingRatesHeader(
            selectedSortOption = ShippingSortOption.CHEAPEST,
            onSortOptionSelected = {},
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
private fun SortingDropdownMenu(
    selectedSortOption: ShippingSortOption,
    onSortOptionSelected: (ShippingSortOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = ShippingSortOption.entries

    Box {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .then(modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(selectedSortOption.stringResource),
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.primary
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = stringResource(
                    R.string.sorted_by,
                    stringResource(selectedSortOption.stringResource)
                ),
                tint = MaterialTheme.colors.primary
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.sizeIn(minWidth = 150.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(onClick = {
                    onSortOptionSelected(option)
                    expanded = false
                }) {
                    Text(
                        text = stringResource(option.stringResource),
                        style = MaterialTheme.typography.subtitle1
                    )
                }
            }
        }
    }
}

@Composable
fun ShippingRates(
    shippingRates: Map<CarrierUI, List<ShippingRateUI>>,
    selectedRate: ShippingRateUI?,
    onSelectedSippingRateChanged: (rate: ShippingRateUI) -> Unit,
    onSelectedRateOptionChanged: (ShippingRateOption, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    tabModifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState { shippingRates.keys.size }
    val scope = rememberCoroutineScope()
    val carriers = shippingRates.keys.toList()

    ScrollableTabRow(
        selectedTabIndex = pagerState.currentPage,
        edgePadding = dimensionResource(R.dimen.major_100),
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.primary,
        divider = {},
        modifier = tabModifier
    ) {
        shippingRates.keys.forEachIndexed { index, carrier ->
            val textColor = if (index == pagerState.currentPage) {
                MaterialTheme.colors.primary
            } else {
                colorResource(id = R.color.color_on_surface_medium)
            }
            LeadingIconTab(
                text = {
                    Text(
                        text = carrier.name,
                        color = textColor,
                        style = MaterialTheme.typography.subtitle2
                    )
                },
                icon = {
                    carrier.logoRes?.let { CarrierLogo(it) }
                },
                selected = pagerState.currentPage == index,
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )
        }
    }

    Divider(modifier = Modifier.fillMaxWidth())

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) { page ->
        val carrier = carriers[page]
        val rates = shippingRates[carrier] ?: emptyList()
        Column {
            rates.forEach { rate ->
                ShippingRateItem(
                    carrier = carrier,
                    shippingRate = rate,
                    selectedRate = selectedRate,
                    onSelectedRateOptionChanged = onSelectedRateOptionChanged,
                    modifier = Modifier.clickable { onSelectedSippingRateChanged(rate) }
                )
            }
        }
    }
}

@Composable
private fun CarrierLogo(
    @DrawableRes resId: Int,
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(id = resId),
        contentDescription = null,
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(5.dp)),
        tint = Color.Unspecified
    )
}

@Composable
private fun ShippingRateItem(
    carrier: CarrierUI,
    shippingRate: ShippingRateUI,
    selectedRate: ShippingRateUI?,
    onSelectedRateOptionChanged: (ShippingRateOption, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = selectedRate?.id == shippingRate.id
    val borderWidth = if (isSelected) {
        2.dp
    } else {
        1.dp
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colors.primary
    } else {
        colorResource(R.color.divider_color)
    }

    val backgroundColor = if (isSelected) {
        animateColorAsState(
            targetValue = colorResource(R.color.woo_item_selected),
            label = "colorAnimation"
        )
    } else {
        animateColorAsState(targetValue = MaterialTheme.colors.surface, label = "colorAnimation")
    }

    RoundedCornerBoxWithBorder(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        innerModifier = modifier,
        backgroundColor = backgroundColor.value,
        borderWidth = borderWidth,
        borderColor = borderColor
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            carrier.logoRes?.let {
                CarrierLogo(resId = it)
            }
            Column(modifier = Modifier.animateContentSize()) {
                Row {
                    Text(
                        text = shippingRate.title,
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )
                    Text(
                        text = shippingRate.formattedBasePrice,
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isSelected) {
                    ShippingRateItemExpandedDescription(
                        shippingRate = selectedRate!!,
                        onSelectedRateOptionChanged = onSelectedRateOptionChanged,
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 16.dp)
                    )
                } else {
                    Text(
                        text = getShippingRateFormattedDescription(LocalContext.current, shippingRate),
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 16.dp)
                    )
                }
            }
        }
    }
}

private fun getShippingRateFormattedDescription(
    context: Context,
    shippingRate: ShippingRateUI
): AnnotatedString {
    return buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(shippingRate.formattedEstimatedDays)
        }
        val options = shippingRate.shippingRateIncludedOptions
        if (options.isNotEmpty()) {
            append(" • ")
            val include = context.getString(
                R.string.shipping_label_rate_included_options,
                options.joinToString().lowercase()
            )
            append(include)
        }
    }
}

@Composable
private fun ShippingRateItemExpandedDescription(
    shippingRate: ShippingRateUI,
    onSelectedRateOptionChanged: (ShippingRateOption, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = shippingRate.shippingRateIncludedOptions
    Column(modifier = modifier) {
        Text(
            text = shippingRate.formattedEstimatedDays,
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(top = 8.dp, end = 16.dp, bottom = 8.dp),
            fontWeight = FontWeight.Bold
        )
        options.forEach {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Done,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = it.capitalize(Locale.current),
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        ShippingRateOptions(
            options = shippingRate.options,
            currentSelectedOptions = listOf(shippingRate.selectedOption) + shippingRate.additionalSelectedOptions,
            onSelectedOption = onSelectedRateOptionChanged,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun ShippingRateOptions(
    options: Map<ShippingRateOption, ShippingRateOptionUI>,
    currentSelectedOptions: List<ShippingRateOption>,
    onSelectedOption: (selectedOption: ShippingRateOption, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        options.filter { it.key != ShippingRateOption.DEFAULT }
            .forEach { option ->
                val isSelected = currentSelectedOptions.contains(option.key)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .toggleable(
                            value = isSelected,
                            onValueChange = { isChecked ->
                                onSelectedOption(option.key, isChecked)
                            }
                        )
                        .padding(vertical = 8.dp)
                        .fillMaxWidth()
                ) {
                    SelectionCheck(
                        isSelected = isSelected,
                        onSelectionChange = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = option.value.feeDescription,
                        style = MaterialTheme.typography.body1,
                    )
                }
            }
    }
}

enum class ShippingSortOption(@StringRes val stringResource: Int) {
    CHEAPEST(R.string.shipping_label_shipping_rates_sort_option_cheapest),
    FASTEST(R.string.shipping_label_shipping_rates_sort_option_fastest)
}

typealias ShippingRateOption = WooShippingRateModel.Option

@Parcelize
data class CarrierUI(
    val carrier: WooShippingCarrier,
    val name: String,
    val logoRes: Int? = null,
) : Parcelable

@Parcelize
data class ShippingRateUI(
    val title: String,
    val formattedBasePrice: String,
    val shippingRateIncludedOptions: List<String>,
    val formattedEstimatedDays: String,
    val options: Map<ShippingRateOption, ShippingRateOptionUI>,
    val selectedOption: ShippingRateOption,
    val additionalSelectedOptions: List<ShippingRateOption>,
) : Parcelable {
    val id: String
        get() = defaultRate.rate.rateId
    val defaultRate: ShippingRateOptionUI
        get() = options.getValue(ShippingRateOption.DEFAULT)
    val selectedRateOption: ShippingRateOptionUI
        get() = options.getValue(selectedOption)
}

@Parcelize
data class ShippingRateOptionUI(
    val option: ShippingRateOption,
    val optionName: String,
    val fee: BigDecimal,
    val formattedFee: String,
    val feeDescription: String,
    val rate: WooShippingRateModel
) : Parcelable
