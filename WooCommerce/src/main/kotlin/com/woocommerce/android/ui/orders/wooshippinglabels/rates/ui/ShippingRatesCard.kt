package com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Colors
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.SelectionCheck
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.RoundedCornerBoxWithBorder
import com.woocommerce.android.ui.orders.wooshippinglabels.ShippingRateSummaryUI
import com.woocommerce.android.ui.orders.wooshippinglabels.models.WooShippingCarrier
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel.Option
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlin.random.Random

@Suppress("MagicNumber")
val Colors.shippingSelectedBackgroundColor: Color get() = if (isLight) Color(0xFFF2EDFF) else Color(0x22F2EDFF)

@Composable
internal fun ShippingRatesCard(
    selectedRate: ShippingRateUI?,
    shippingRates: Map<CarrierUI, List<ShippingRateUI>>,
    selectedSortOption: ShippingSortOption,
    onSelectedRateSortOrderChanged: (ShippingSortOption) -> Unit,
    onSelectedSippingRateChanged: (rate: ShippingRateUI) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ShippingRatesHeader(
            selectedSortOption = selectedSortOption,
            onSortOptionSelected = onSelectedRateSortOrderChanged,
            modifier = Modifier.padding(start = dimensionResource(R.dimen.major_100))
        )
        ShippingRates(
            selectedRate = selectedRate,
            shippingRates = shippingRates,
            onSelectedSippingRateChanged = onSelectedSippingRateChanged
        )
    }
}

@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.PIXEL)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.PIXEL)
@Composable
private fun ShippingRatesCardPreview() {
    val rates = generateShippingRates()
    val selected = rates.values.first().first()
    WooThemeWithBackground {
        ShippingRatesCard(
            selectedRate = selected,
            shippingRates = generateShippingRates(),
            selectedSortOption = ShippingSortOption.CHEAPEST,
            onSelectedRateSortOrderChanged = {},
            onSelectedSippingRateChanged = {}
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 300.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
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
    selectedRate: ShippingRateUI?,
    shippingRates: Map<CarrierUI, List<ShippingRateUI>>,
    onSelectedSippingRateChanged: (rate: ShippingRateUI) -> Unit,
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
                    onSelectedSippingRateChanged = onSelectedSippingRateChanged,
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
    onSelectedSippingRateChanged: (rate: ShippingRateUI) -> Unit,
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
            targetValue = MaterialTheme.colors.shippingSelectedBackgroundColor,
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
                        text = shippingRate.selectedOption.title,
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )
                    Text(
                        text = shippingRate.selectedOption.formatedPrice,
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isSelected) {
                    ShippingRateItemExpandedDescription(
                        shippingRate = shippingRate,
                        selectedRate = selectedRate,
                        onSelectedSippingRateChanged = onSelectedSippingRateChanged,
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
            append(shippingRate.defaultRate.formattedEstimatedDays)
        }
        val options = shippingRate.defaultRate.shippingRateOptions
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
    selectedRate: ShippingRateUI?,
    onSelectedSippingRateChanged: (rate: ShippingRateUI) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = shippingRate.defaultRate.shippingRateOptions
    Column(modifier = modifier) {
        Text(
            text = shippingRate.defaultRate.formattedEstimatedDays,
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
        SelectSignatureRequired(
            options = shippingRate.options,
            currentSelectedOption = selectedRate?.selectedOption,
            onSelectedOption = { option ->
                val selection = if (option != selectedRate?.selectedOption) {
                    option
                } else {
                    shippingRate.defaultRate
                }
                onSelectedSippingRateChanged(shippingRate.copy(selectedOption = selection))
            },
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun SelectSignatureRequired(
    options: Map<Option, ShippingRateOptionUI>,
    currentSelectedOption: ShippingRateOptionUI?,
    onSelectedOption: (selectedOption: ShippingRateOptionUI) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        options.filter { it.key != Option.DEFAULT }
            .forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onSelectedOption(option.value) }
                        .padding(vertical = 8.dp)
                        .fillMaxWidth()
                ) {
                    SelectionCheck(
                        isSelected = option.value.rate.rateId == currentSelectedOption?.rate?.rateId,
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

data class CarrierUI(
    val carrier: WooShippingCarrier,
    val name: String,
    val logoRes: Int? = null,
)

data class ShippingRateUI(
    val options: Map<Option, ShippingRateOptionUI>,
    val selectedOption: ShippingRateOptionUI
) {
    val id = defaultRate.rate.rateId
    val defaultRate: ShippingRateOptionUI
        get() = options[Option.DEFAULT] ?: options.values.first()

    val summary = ShippingRateSummaryUI(
        serviceName = selectedOption.title,
        total = selectedOption.formatedPrice,
        optionName = selectedOption.formattedOptionName,
        optionFee = selectedOption.formattedFee
    )
}

data class ShippingRateOptionUI(
    val title: String,
    val formatedPrice: String,
    val formattedFee: String,
    val formattedOptionName: String,
    val feeDescription: String,
    val formattedEstimatedDays: String,
    val option: Option,
    val shippingRateOptions: List<String>,
    val rate: WooShippingRateModel
)

fun generateShippingRates(): Map<CarrierUI, List<ShippingRateUI>> {
    val carriers = listOf(
        CarrierUI(
            carrier = WooShippingCarrier.DHL,
            name = "DHL Express",
            logoRes = R.drawable.dhl_logo
        ),
        CarrierUI(
            carrier = WooShippingCarrier.USPS,
            name = "USPS",
            logoRes = R.drawable.usps_logo
        ),
        CarrierUI(
            carrier = WooShippingCarrier.UPS,
            name = "UPS",
            logoRes = R.drawable.ups_logo
        ),
        CarrierUI(
            carrier = WooShippingCarrier.FEDEX,
            name = "Fed Ex",
            logoRes = R.drawable.fedex_logo
        ),
        CarrierUI(
            carrier = WooShippingCarrier.UNKNOWN,
            name = "Canada Post",
            logoRes = null
        )
    )

    return carriers.associateWith {
        generateRates(
            it.carrier,
            Random(0).nextInt(from = 3, until = 10)
        )
    }
}

fun generateRates(carrier: WooShippingCarrier, number: Int): List<ShippingRateUI> {
    return List(number) {
        val rate = WooShippingRateModel(
            packageId = "123$it",
            shipmentId = "123$it",
            rateId = "123$it",
            serviceId = "123$it",
            carrierId = "123$it",
            serviceName = "$carrier $it",
            deliveryDays = it,
            price = it.toBigDecimal(),
            discount = it.toBigDecimal(),
            option = Option.DEFAULT,
            carrier = WooShippingCarrier.DHL,
            hasFreePickup = true,
            insurance = BigDecimal.TEN,
            isTrackingEnabled = true,
            deliveryDate = null,
            isDeliveryDateGuaranteed = false,
            isSelected = false,
            listRate = BigDecimal.TEN,
            retailRate = BigDecimal.TEN
        )
        val option = ShippingRateOptionUI(
            title = rate.serviceName,
            formatedPrice = rate.price.toString(),
            formattedFee = rate.price.toString(),
            option = rate.option,
            rate = rate,
            shippingRateOptions = listOf(
                "Tracking",
                "Insurance",
                "Free Pickup"
            ),
            formattedEstimatedDays = "$it business days",
            formattedOptionName = "Default",
            feeDescription = "Default"
        )
        val options = mapOf(Option.DEFAULT to option)

        ShippingRateUI(
            options = options,
            selectedOption = options.values.first()
        )
    }
}
