package com.woocommerce.android.ui.products

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
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WcExposedDropDown

@Composable
fun UpdateProductStockStatusScreen(viewModel: UpdateProductStockStatusViewModel) {
    val uiState by viewModel.viewState.observeAsState(UpdateProductStockStatusViewModel.UpdateStockStatusUiState())

    UpdateProductStockStatusScreen(
        stockStatusInfos = uiState.stockStatusInfos,
        isMixedStatus = uiState.isMixedStatus,
        productsToUpdateCount = uiState.productsToUpdateCount,
        ignoredProductsCount = uiState.ignoredProductsCount,
        updateResult = uiState.updateResult,
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
    updateResult: RequestResult?,
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
                stockStatusOptions = stockStatusInfos.map { it.stockStatus.value },
                onSelectionChanged = { newStatus ->
                    val status = ProductStockStatus.fromString(newStatus)
                    onStockStatusChanged(status, productIds)
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
fun StockStatusDropdown(stockStatusOptions: List<String>, onSelectionChanged: (String) -> Unit) {
    val initialStatus = stockStatusOptions.firstOrNull() ?: ""

    WcExposedDropDown(
        items = stockStatusOptions.toTypedArray(),
        onSelected = onSelectionChanged,
        currentValue = initialStatus,
        mapper = { it }
    )
}
