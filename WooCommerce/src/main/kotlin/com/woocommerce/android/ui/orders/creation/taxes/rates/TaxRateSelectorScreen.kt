package com.woocommerce.android.ui.orders.creation.taxes.rates

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.InfiniteListHandler
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun TaxRateSelectorScreen(
    viewState: StateFlow<TaxRateSelectorViewModel.ViewState>,
    onEditTaxRatesInAdminClicked: () -> Unit,
    onInfoIconClicked: () -> Unit,
    onTaxRateClick: (TaxRateSelectorViewModel.TaxRateUiModel) -> Unit,
    onDismiss: () -> Unit,
    onLoadMore: () -> Unit,
    onEmptyScreenButtonClicked: () -> Unit,
) {
    Scaffold(
        backgroundColor = MaterialTheme.colors.surface,
        topBar = { Toolbar(onDismiss, onInfoIconClicked) }
    ) {
        TaxRates(
            Modifier.padding(it),
            viewState.collectAsState().value,
            onInfoIconClicked,
            onTaxRateClick,
            onEditTaxRatesInAdminClicked,
            onLoadMore,
            onEmptyScreenButtonClicked,
        )
    }
}

@Composable
fun EmptyTaxRateSelectorList(
    onButtonClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(id = R.dimen.major_200)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.tax_rate_empty_list_title),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(
                start = dimensionResource(id = R.dimen.major_150),
                end = dimensionResource(id = R.dimen.major_150)
            )
        )
        Spacer(Modifier.size(dimensionResource(id = R.dimen.major_325)))
        Image(
            painter = painterResource(id = R.drawable.img_empty_tax),
            contentDescription = null,
        )
        Spacer(Modifier.size(dimensionResource(id = R.dimen.major_200)))
        Text(
            text = stringResource(id = R.string.tax_rate_empty_list_message),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(
                start = dimensionResource(id = R.dimen.major_150),
                end = dimensionResource(id = R.dimen.major_150)
            )
        )
        Spacer(Modifier.size(dimensionResource(id = R.dimen.major_325)))
        WCColoredButton(
            onClick = onButtonClicked,
            text = stringResource(id = R.string.tax_rate_empty_list_button),
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_100)),
        )
    }
}

@Composable
private fun Toolbar(onDismiss: () -> Unit, onInfoIconClicked: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.tax_rate_selector_title)) },
        navigationIcon = {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = stringResource(R.string.close),
                )
            }
        },
        backgroundColor = colorResource(id = R.color.color_toolbar),
        elevation = 0.dp,
        actions = {
            IconButton(onClick = onInfoIconClicked) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_info_outline_20dp),
                    contentDescription = stringResource(R.string.tax_rate_selector_info_icon_content_description),
                    tint = MaterialTheme.colors.primary,
                )
            }
            Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.major_100)))
        }
    )
}

@Composable
private fun TaxRates(
    modifier: Modifier = Modifier,
    state: TaxRateSelectorViewModel.ViewState,
    onInfoIconClicked: () -> Unit,
    onTaxRateClick: (TaxRateSelectorViewModel.TaxRateUiModel) -> Unit,
    onEditTaxRatesInAdminClicked: () -> Unit,
    onLoadMore: () -> Unit = {},
    onEmptyScreenButtonClicked: () -> Unit,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Header(onInfoIconClicked)
        }
        when {
            state.taxRates.isEmpty() -> {
                item {
                    EmptyTaxRateSelectorList(onEmptyScreenButtonClicked)
                }
            }
            else -> {
                item {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = dimensionResource(id = R.dimen.major_100),
                                vertical = dimensionResource(
                                    id = R.dimen.minor_100
                                )
                            ),
                        fontSize = 13.sp,
                        color = colorResource(id = R.color.woo_gray_40),
                        text = stringResource(R.string.tax_rate_selector_list_header)
                    )
                    Divider()
                }
                itemsIndexed(state.taxRates) { _, taxRate ->
                    TaxRateRow(taxRate, onTaxRateClick)
                }
                item {
                    Footer(onEditTaxRatesInAdminClicked)
                }
            }
        }
        if (state.isLoading) {
            item {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth()
                        .padding(vertical = dimensionResource(id = R.dimen.minor_100))
                )
            }
        }
    }
    InfiniteListHandler(listState = listState) {
        onLoadMore()
    }
}

@Composable
fun TaxRateRow(
    taxRate: TaxRateSelectorViewModel.TaxRateUiModel,
    onClick: (TaxRateSelectorViewModel.TaxRateUiModel) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick(taxRate)
            }
            .padding(dimensionResource(id = R.dimen.major_100)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = taxRate.label,
            fontSize = dimensionResource(id = R.dimen.text_minor_125).value.sp,
        )
        Text(
            modifier = Modifier.padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.minor_00)
            ),
            fontSize = dimensionResource(id = R.dimen.text_minor_125).value.sp,
            text = taxRate.rate
        )
    }
    Divider()
}

@Composable
fun Header(onInfoIconClicked: () -> Unit) {
    Box(modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(
                        dimensionResource(id = R.dimen.minor_10),
                        colorResource(id = R.color.woo_gray_80_alpha_012)
                    ),
                    RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_large))
                ),
        ) {
            IconButton(
                modifier = Modifier.padding(dimensionResource(id = R.dimen.minor_25)),
                onClick = onInfoIconClicked
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_info_outline_20dp),
                    tint = MaterialTheme.colors.primary,
                    contentDescription = stringResource(R.string.tax_rate_selector_info_icon_content_description)
                )
            }
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = dimensionResource(id = R.dimen.minor_00),
                        top = dimensionResource(id = R.dimen.major_100),
                        end = dimensionResource(id = R.dimen.major_100),
                        bottom = dimensionResource(id = R.dimen.major_100)
                    ),
                style = MaterialTheme.typography.body2,
                text = stringResource(R.string.tax_rate_selector_header_label),
            )
        }
    }
}

@Composable
private fun Footer(
    onEditTaxRatesInAdminClicked: () -> Unit
) {
    Text(
        modifier = Modifier.padding(
            start = dimensionResource(id = R.dimen.major_100),
            end = dimensionResource(id = R.dimen.major_100),
            top = dimensionResource(id = R.dimen.major_150),
            bottom = dimensionResource(id = R.dimen.minor_00)
        ),
        style = MaterialTheme.typography.caption,
        color = colorResource(id = R.color.woo_gray_40),
        text = stringResource(R.string.tax_rate_selector_footer_label)
    )
    EditTaxRatesInAdminButton(onEditTaxRatesInAdminClicked)
}

@Composable
fun EditTaxRatesInAdminButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Text(stringResource(R.string.tax_rate_selector_edit_rates_button_label))
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_external),
                contentDescription = null,
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.image_minor_50))
                    .align(Alignment.CenterVertically)
                    .padding(
                        dimensionResource(id = R.dimen.minor_50),
                    )

            )
        }
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TaxRateSelectorScreenPreview() = WooThemeWithBackground {
    val viewState = TaxRateSelectorViewModel.ViewState(
        taxRates = listOf(
            TaxRateSelectorViewModel.TaxRateUiModel(
                label = "Government Sales Tax · US CA 94016 San Francisco",
                rate = "20%",
                taxRate = TaxRate(0)
            ),
            TaxRateSelectorViewModel.TaxRateUiModel(
                label = "GST · US CA",
                rate = "5%",
                taxRate = TaxRate(0)
            ),
            TaxRateSelectorViewModel.TaxRateUiModel(
                label = "GST · AU",
                rate = "0%",
                taxRate = TaxRate(0)
            ),
        )
    )
    val state = MutableStateFlow(viewState)
    TaxRateSelectorScreen(
        viewState = state,
        onEditTaxRatesInAdminClicked = {},
        onInfoIconClicked = {},
        onTaxRateClick = {},
        onDismiss = {},
        onLoadMore = {},
        onEmptyScreenButtonClicked = {},
    )
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FooterPreview() = WooThemeWithBackground {
    Footer(onEditTaxRatesInAdminClicked = {})
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TaxRatesPreview() = WooThemeWithBackground {
    val viewState = TaxRateSelectorViewModel.ViewState(
        isLoading = true,
        taxRates = listOf(
            TaxRateSelectorViewModel.TaxRateUiModel(
                label = "Government Sales Tax · US CA 94016 San Francisco",
                rate = "20%",
                taxRate = TaxRate(0)
            ),
            TaxRateSelectorViewModel.TaxRateUiModel(
                label = "GST · US CA",
                rate = "5%",
                taxRate = TaxRate(0)
            ),
            TaxRateSelectorViewModel.TaxRateUiModel(
                label = "GST · AU",
                rate = "0%",
                taxRate = TaxRate(0)
            ),
        )
    )
    val state by remember { mutableStateOf(viewState) }
    TaxRates(
        state = state,
        onTaxRateClick = {},
        onEditTaxRatesInAdminClicked = {},
        onInfoIconClicked = {},
        onLoadMore = {},
        onEmptyScreenButtonClicked = {},
    )
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TaxRateEmptyListPreview() = WooThemeWithBackground {
    EmptyTaxRateSelectorList(onButtonClicked = {})
}
