package com.woocommerce.android.ui.products

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.ui.compose.component.Toolbar

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
        onStockStatusesLoaded = { productIds ->
            viewModel.loadProductStockStatuses(productIds)
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
    onStockStatusesLoaded: (List<Long>) -> Unit,
    onNavigationUpClicked: () -> Unit,
    onUpdateClicked: () -> Unit
) {
    Scaffold(
        topBar = {
            Toolbar(
                title = "Update Stock Status",
                onNavigationButtonClick = onNavigationUpClicked,
                actions = {
                    TextButton(onClick = onUpdateClicked) {
                        Text("DONE", color = MaterialTheme.colors.onPrimary)
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
                    "Current stock statuses are mixed"
                } else {
                    "Current stock status is ${stockStatusInfos.firstOrNull()?.stockStatus?.value ?: "unknown"}"
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            )

            Text(
                text = buildString {
                    append("Stock status will be updated for $productsToUpdateCount products.")
                    if (ignoredProductsCount > 0) {
                        append(" $ignoredProductsCount products with managed stock quantity will be ignored.")
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

    Text("Dropdown Placeholder")
}
