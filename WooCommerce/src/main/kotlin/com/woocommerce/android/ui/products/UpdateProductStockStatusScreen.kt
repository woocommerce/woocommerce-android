package com.woocommerce.android.ui.products

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCExposedDropDown
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.Companion.AVAILABLE_STOCK_STATUSES
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.StockStatusState
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.StockStatusState.Common
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.StockStatusState.Mixed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateProductStockStatusScreen(
    currentStockStatusState: StockStatusState,
    statusMessage: String,
    currentProductStockStatus: ProductStockStatus,
    stockStatuses: List<ProductStockStatus>,
    isProgressDialogVisible: Boolean,
    onStockStatusChanged: (ProductStockStatus) -> Unit,
    onNavigationUpClicked: () -> Unit,
    onUpdateClicked: () -> Unit
) {
    val borderColor = colorResource(id = R.color.divider_color)

    val currentStockStatusMessage = when (currentStockStatusState) {
        is Mixed ->
            stringResource(id = R.string.product_update_stock_status_current_status_mixed)

        is Common -> stringResource(
            id = R.string.product_update_stock_status_current_status_single,
            stringResource(currentStockStatusState.status.stringResource)
        )
    }

    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.product_update_stock_status_title),
                onNavigationButtonClick = onNavigationUpClicked,
                onActionButtonClick = onUpdateClicked,
                actionButtonText = stringResource(id = R.string.product_update_stock_status_done),
                windowInsets = TopAppBarDefaults.windowInsets,
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Spacer(modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_150)))
            StockStatusDropdown(
                currentProductStockStatus = currentProductStockStatus,
                stockStatuses = stockStatuses,
                onSelectionChanged = { newStatus ->
                    onStockStatusChanged(newStatus)
                },
                modifier = Modifier
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            )

            Spacer(modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100)))

            Text(
                text = currentStockStatusMessage,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(start = dimensionResource(id = R.dimen.major_75)),
                style = MaterialTheme.typography.titleSmall,
                color = colorResource(id = R.color.color_on_surface_medium)
            )

            Spacer(modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100)))

            HorizontalDivider(color = borderColor)

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.titleSmall,
                color = colorResource(id = R.color.color_on_surface_disabled),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .padding(
                        vertical = dimensionResource(id = R.dimen.major_75),
                        horizontal = dimensionResource(id = R.dimen.major_100)
                    ),
                textAlign = TextAlign.Center
            )

            HorizontalDivider(color = borderColor)
        }

        if (isProgressDialogVisible) {
            ProgressDialog(
                title = stringResource(id = R.string.product_update_stock_status_dialog_title),
                subtitle = stringResource(id = R.string.product_update_stock_status_dialog_subtitle)
            )
        }
    }
}

@Composable
fun StockStatusDropdown(
    currentProductStockStatus: ProductStockStatus,
    stockStatuses: List<ProductStockStatus>,
    onSelectionChanged: (ProductStockStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dropdownFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        dropdownFocusRequester.requestFocus()
    }

    val displayStringToStatusMap = stockStatuses.associateBy {
        stringResource(id = it.stringResource)
    }

    val initialStatusDisplayString = stringResource(id = currentProductStockStatus.stringResource)

    WCExposedDropDown(
        items = displayStringToStatusMap.keys,
        onSelected = { selectedString ->
            displayStringToStatusMap[selectedString]?.let { onSelectionChanged(it) }
        },
        currentValue = initialStatusDisplayString,
        mapper = { it },
        modifier = modifier,
        focusRequester = dropdownFocusRequester,
        fillWidth = true
    )
}

@Composable
@Preview(name = "Single Status - Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Single Status - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun UpdateProductStockStatusSingleStatusPreview() {
    WooThemeWithBackground {
        UpdateProductStockStatusScreen(
            currentStockStatusState = Common(ProductStockStatus.InStock),
            statusMessage = "5 products will be updated.",
            currentProductStockStatus = ProductStockStatus.InStock,
            isProgressDialogVisible = false,
            stockStatuses = AVAILABLE_STOCK_STATUSES,
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
            statusMessage = "5 products will be updated.",
            currentProductStockStatus = ProductStockStatus.InStock,
            stockStatuses = AVAILABLE_STOCK_STATUSES,
            isProgressDialogVisible = false,
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
            statusMessage = "5 products will be updated.",
            currentProductStockStatus = ProductStockStatus.OutOfStock,
            isProgressDialogVisible = false,
            stockStatuses = AVAILABLE_STOCK_STATUSES,
            onStockStatusChanged = { },
            onNavigationUpClicked = { },
            onUpdateClicked = { }
        )
    }
}
