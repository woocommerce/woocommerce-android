package com.woocommerce.android.ui.products

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WcExposedDropDown
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun UpdateProductStockStatusScreen(viewModel: UpdateProductStockStatusViewModel) {
    val uiState by viewModel.viewState.observeAsState(UpdateProductStockStatusViewModel.UpdateStockStatusUiState())

    UpdateProductStockStatusScreen(
        stockStatusInfos = uiState.stockStatusInfos,
        isMixedStatus = uiState.isMixedStatus,
        productsToUpdateCount = uiState.productsToUpdateCount,
        ignoredProductsCount = uiState.ignoredProductsCount,
        updateResult = uiState.updateResult,
        initialProductStockStatus = uiState.initialProductStockStatus,
        onStockStatusChanged = { newStatus, productIds ->
            viewModel.updateStockStatusForProducts(newStatus, productIds)
        },
        onNavigationUpClicked = { },
        onUpdateClicked = { }
    )
}

@Composable
private fun UpdateProductStockStatusScreen(
    stockStatusInfos: List<UpdateProductStockStatusViewModel.ProductStockStatusInfo>,
    isMixedStatus: Boolean,
    productsToUpdateCount: Int,
    ignoredProductsCount: Int,
    @Suppress("UNUSED_PARAMETER") updateResult: RequestResult?,
    initialProductStockStatus: ProductStockStatus,
    onStockStatusChanged: (ProductStockStatus, List<Long>) -> Unit,
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
                            color = MaterialTheme.colors.onPrimary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            val productIds = stockStatusInfos.map { it.productId }
            StockStatusDropdown(
                initialProductStockStatus = initialProductStockStatus, // Pass the initial status to the dropdown
                onSelectionChanged = { newStatus ->
                    onStockStatusChanged(newStatus, productIds)
                }
            )

            Text(
                text = if (isMixedStatus) {
                    stringResource(id = R.string.product_update_stock_status_current_status_mixed)
                } else {
                    stringResource(
                        id = R.string.product_update_stock_status_current_status_single,
                        stockStatusInfos.firstOrNull()?.stockStatus?.value ?: "unknown"
                    )
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            )

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
                    .padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun StockStatusDropdown(
    initialProductStockStatus: ProductStockStatus,
    onSelectionChanged: (ProductStockStatus) -> Unit
) {
    val stockStatusOptions = listOf(
        ProductStockStatus.InStock,
        ProductStockStatus.OutOfStock,
        ProductStockStatus.OnBackorder,
        ProductStockStatus.InsufficientStock
    )

    val displayStringToStatusMap = stockStatusOptions.associateBy {
        stringResource(id = it.stringResource)
    }

    val initialStatusDisplayString = stringResource(id = initialProductStockStatus.stringResource)

    WcExposedDropDown(
        items = displayStringToStatusMap.keys.toTypedArray(),
        onSelected = { selectedString ->
            displayStringToStatusMap[selectedString]?.let { onSelectionChanged(it) }
        },
        currentValue = initialStatusDisplayString,
        mapper = { it }
    )
}

@Composable
@Preview(name = "Single Status - Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Single Status - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun UpdateProductStockStatusSingleStatusPreview() {
    WooThemeWithBackground {
        UpdateProductStockStatusScreen(
            stockStatusInfos = listOf(
                UpdateProductStockStatusViewModel.ProductStockStatusInfo(1, ProductStockStatus.InStock, false)
            ),
            isMixedStatus = false,
            productsToUpdateCount = 10,
            ignoredProductsCount = 0,
            updateResult = null,
            initialProductStockStatus = ProductStockStatus.InStock,
            onStockStatusChanged = { _, _ -> },
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
            stockStatusInfos = listOf(
                UpdateProductStockStatusViewModel.ProductStockStatusInfo(1, ProductStockStatus.InStock, false),
                UpdateProductStockStatusViewModel.ProductStockStatusInfo(2, ProductStockStatus.OnBackorder, false)
            ),
            isMixedStatus = true,
            productsToUpdateCount = 10,
            ignoredProductsCount = 0,
            updateResult = null,
            initialProductStockStatus = ProductStockStatus.InStock,
            onStockStatusChanged = { _, _ -> },
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
            stockStatusInfos = listOf(
                UpdateProductStockStatusViewModel.ProductStockStatusInfo(1, ProductStockStatus.InStock, true),
                UpdateProductStockStatusViewModel.ProductStockStatusInfo(2, ProductStockStatus.OutOfStock, false)
            ),
            isMixedStatus = false,
            productsToUpdateCount = 1,
            ignoredProductsCount = 1,
            updateResult = null,
            initialProductStockStatus = ProductStockStatus.InStock,
            onStockStatusChanged = { _, _ -> },
            onNavigationUpClicked = { },
            onUpdateClicked = { }
        )
    }
}
