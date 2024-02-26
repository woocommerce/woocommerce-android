package com.woocommerce.android.ui.products

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.component.Toolbar

@Composable
fun UpdateProductStockStatusScreen(viewModel: UpdateProductStockStatusViewModel, onNavigationUpClicked: () -> Unit, onUpdateClicked: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Toolbar(
                title = "Update Stock Status",
                onNavigationUpClicked = onNavigationUpClicked,
                actions = {
                    TextButton(onClick = onUpdateClicked) {
                        Text("DONE", color = MaterialTheme.colors.onPrimary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            StockStatusDropdown(
                stockStatusOptions = listOf("In stock", "Out of stock", "On backorder"),
                onSelectionChanged = { newStatus ->
                    viewModel.updateStockStatus(newStatus)
                }
            )

            if (uiState.isMixedStatus) {
                Text(
                    "Current stock statuses are mixed",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                )
            } else {
                Text(
                    "Current stock status is ${uiState.currentStatus}",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                )
            }

            Text(
                "Stock status will be updated for ${uiState.productsToUpdateCount} products." +
                    if (uiState.ignoredProductsCount > 0) " ${uiState.ignoredProductsCount} products with managed stock quantity will be ignored." else "",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun StockStatusDropdown(stockStatusOptions: List<String>, onSelectionChanged: (String) -> Unit) {
    // Implementation of the dropdown
    // This is a placeholder for the actual dropdown implementation
    Text("Dropdown Placeholder")
}
