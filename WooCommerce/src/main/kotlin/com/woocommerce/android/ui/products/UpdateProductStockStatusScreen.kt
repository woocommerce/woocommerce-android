package com.woocommerce.android.ui.products

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WcExposedDropDown
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.StockStatusState
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.StockStatusState.Common
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.StockStatusState.Mixed
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.UpdateStockStatusUiState

@Composable
fun UpdateProductStockStatusScreen(viewModel: UpdateProductStockStatusViewModel) {
    val uiState by viewModel.viewState.observeAsState(UpdateStockStatusUiState())

    UpdateProductStockStatusScreen(
        currentStockStatusState = uiState.currentStockStatusState,
        productsToUpdateCount = uiState.productsToUpdateCount,
        ignoredProductsCount = uiState.ignoredProductsCount,
//        updateResult = uiState.updateResult,
        initialProductStockStatus = uiState.initialProductStockStatus,
        onStockStatusChanged = { newStatus ->
            viewModel.updateStockStatusForProducts(newStatus)
        },
        onNavigationUpClicked = { viewModel.onBackPressed() },
        onUpdateClicked = { }
    )
}

@Composable
private fun UpdateProductStockStatusScreen(
    currentStockStatusState: StockStatusState,
    productsToUpdateCount: Int,
    ignoredProductsCount: Int,
//    @Suppress("UNUSED_PARAMETER") updateResult: RequestResult?,
    initialProductStockStatus: ProductStockStatus,
    onStockStatusChanged: (ProductStockStatus) -> Unit,
    onNavigationUpClicked: () -> Unit,
    onUpdateClicked: () -> Unit
) {
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.product_update_stock_status_title),
                onNavigationButtonClick = onNavigationUpClicked,
                actions = {
                    TextButton(onClick = onUpdateClicked) {
                        Text(
                            text = stringResource(id = R.string.product_update_stock_status_done),
                            color = MaterialTheme.colors.primary
                        )
                    }
                }
            )
        },
        backgroundColor = MaterialTheme.colors.surface
    ) { innerPadding ->
        Spacer(modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100)))
        Column(modifier = Modifier.padding(innerPadding)) {

            StockStatusDropdown(
                initialProductStockStatus = initialProductStockStatus,
                onSelectionChanged = { newStatus ->
                    onStockStatusChanged(newStatus)
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )

            val statusMessage = when (currentStockStatusState) {
                is Mixed ->
                    stringResource(id = R.string.product_update_stock_status_current_status_mixed)

                is Common -> stringResource(
                    id = R.string.product_update_stock_status_current_status_single,
                    currentStockStatusState.status.value
                )
            }
            Spacer(modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100)))

            Text(
                text = statusMessage,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(start = 12.dp),
                style = MaterialTheme.typography.subtitle2
            )

            Spacer(modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100)))

            Divider()

            Text(
                text = buildString {
                    append(
                        stringResource(
                            id = R.string.product_update_stock_status_update_count,
                            productsToUpdateCount
                        )
                    )
                    if (ignoredProductsCount > 0) {
                        append(" ")
                        append(
                            stringResource(
                                id = R.string.product_update_stock_status_ignored_count,
                                ignoredProductsCount
                            )
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 12.dp)
            )

            Divider()

        }
    }
}

@Composable
fun StockStatusDropdown(
    initialProductStockStatus: ProductStockStatus,
    onSelectionChanged: (ProductStockStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stockStatusOptions = listOf(
        ProductStockStatus.InStock,
        ProductStockStatus.OutOfStock,
        ProductStockStatus.OnBackorder,
    )

    val displayStringToStatusMap = stockStatusOptions.associateBy {
        stringResource(id = it.stringResource)
    }

    val initialStatusDisplayString = if (initialProductStockStatus in stockStatusOptions) {
        stringResource(id = initialProductStockStatus.stringResource)
    } else {
        stringResource(id = ProductStockStatus.InStock.stringResource)
    }

    WcExposedDropDown(
        items = displayStringToStatusMap.keys.toTypedArray(),
        onSelected = { selectedString ->
            displayStringToStatusMap[selectedString]?.let { onSelectionChanged(it) }
        },
        currentValue = initialStatusDisplayString,
        mapper = { it },
        modifier = modifier,
    )
}

@Composable
@Preview(name = "Single Status - Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Single Status - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun UpdateProductStockStatusSingleStatusPreview() {
    WooThemeWithBackground {
        UpdateProductStockStatusScreen(
            currentStockStatusState = Common(ProductStockStatus.InStock),
            productsToUpdateCount = 10,
            ignoredProductsCount = 0,
//            updateResult = null,
            initialProductStockStatus = ProductStockStatus.InStock,
            onStockStatusChanged = { },
            onNavigationUpClicked = { },
            onUpdateClicked = { }
        )
    }
}

@Composable
@Preview(name = "Mixed Statuses - Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Mixed Statuses - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun UpdateProductStockStatusMixedStatusPreview() {
    WooThemeWithBackground {
        UpdateProductStockStatusScreen(
            currentStockStatusState = Mixed,
            productsToUpdateCount = 10,
            ignoredProductsCount = 0,
//            updateResult = null,
            initialProductStockStatus = ProductStockStatus.InStock,
            onStockStatusChanged = { },
            onNavigationUpClicked = { },
            onUpdateClicked = { }
        )
    }
}

@Composable
@Preview(name = "Ignored Products - Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Ignored Products - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun UpdateProductStockStatusIgnoredProductsPreview() {
    WooThemeWithBackground {
        UpdateProductStockStatusScreen(
            currentStockStatusState = Common(ProductStockStatus.OutOfStock),
            productsToUpdateCount = 1,
            ignoredProductsCount = 1,
//            updateResult = null,
            initialProductStockStatus = ProductStockStatus.OutOfStock,
            onStockStatusChanged = { },
            onNavigationUpClicked = { },
            onUpdateClicked = { }
        )
    }
}
