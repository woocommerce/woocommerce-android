package com.woocommerce.android.ui.products.inventory

import androidx.camera.core.ImageProxy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.ui.res.colorResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.barcodescanner.BarcodeScannerScreen
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel
import com.woocommerce.android.ui.compose.component.ProgressIndicator
import com.woocommerce.android.ui.compose.component.WCModalBottomSheet
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanToUpdateInventoryScreen(
    onNewFrame: (ImageProxy) -> Unit,
    onBindingException: (Exception) -> Unit,
    permissionState: State<BarcodeScanningViewModel.PermissionState>,
    onCameraPermissionResult: (Boolean) -> Unit,
    viewState: State<ScanToUpdateInventoryViewModel.ViewState>,
    onBottomSheetDismissed: () -> Unit,
    onIncrementQuantityClicked: () -> Unit,
    onUpdateQuantityClicked: () -> Unit,
    onViewProductDetailsClicked: () -> Unit,
    onManualQuantityEntered: (String) -> Unit,
    onManageStockClicked: () -> Unit,
) = WooThemeWithBackground {
    BarcodeScannerScreen(
        onNewFrame = onNewFrame,
        onBindingException = onBindingException,
        permissionState = permissionState,
        onResult = onCameraPermissionResult,
    )
    if (viewState.value is ScanToUpdateInventoryViewModel.ViewState.Loading) {
        ProgressIndicator(backgroundColor = colorResource(id = R.color.color_scrim_background))
    }
    val modalState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    viewState.value.let { state ->
        if (state is ScanToUpdateInventoryViewModel.ViewState.QuickInventoryBottomSheetVisible) {
            WCModalBottomSheet(
                sheetState = modalState,
                onDismissRequest = onBottomSheetDismissed
            ) {
                QuickInventoryUpdateBottomSheet(
                    state = state,
                    onIncrementQuantityClicked = onIncrementQuantityClicked,
                    onManualQuantityEntered = onManualQuantityEntered,
                    onUpdateQuantityClicked = onUpdateQuantityClicked,
                    onViewProductDetailsClicked = onViewProductDetailsClicked,
                    onManageStockClicked = onManageStockClicked,
                )
            }
        }
        LaunchedEffect(state) {
            if (state is ScanToUpdateInventoryViewModel.ViewState.QuickInventoryBottomSheetVisible) {
                modalState.show()
            } else {
                modalState.hide()
            }
        }
    }
}
