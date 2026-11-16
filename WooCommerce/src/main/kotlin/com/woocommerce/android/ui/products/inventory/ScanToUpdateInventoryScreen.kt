package com.woocommerce.android.ui.products.inventory

import androidx.camera.core.ImageProxy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.woocommerce.android.R
import com.woocommerce.android.ui.barcodescanner.BarcodeScannerScreen
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel
import com.woocommerce.android.ui.barcodescanner.DefaultScannerOverlay
import com.woocommerce.android.ui.barcodescanner.HardwareBarcodeScannerInfoSheet
import com.woocommerce.android.ui.barcodescanner.HardwareBarcodeScannerTestFailure
import com.woocommerce.android.ui.compose.component.ProgressIndicator
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCModalBottomSheet
import com.woocommerce.android.ui.compose.theme.LegacyWooThemeWithBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanToUpdateInventoryScreen(
    cameraScanningEnabled: Boolean,
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
    isHardwareScannerInfoVisible: State<Boolean>,
    onHardwareScannerInfoClicked: () -> Unit,
    onHardwareScannerTestScanSucceeded: () -> Unit,
    onHardwareScannerTestScanFailed: (HardwareBarcodeScannerTestFailure) -> Unit,
    onHardwareScannerInfoDismissed: () -> Unit,
) = LegacyWooThemeWithBackground {
    if (cameraScanningEnabled) {
        BarcodeScannerScreen(
            onNewFrame = onNewFrame,
            onBindingException = onBindingException,
            permissionState = permissionState,
            onResult = onCameraPermissionResult,
            overlayContent = {
                DefaultScannerOverlay(
                    onHardwareScannerInfoClicked = onHardwareScannerInfoClicked,
                )
            },
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.major_100)),
            verticalArrangement = Arrangement.spacedBy(
                dimensionResource(R.dimen.major_100),
                Alignment.CenterVertically,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.barcode_scanning_scan_product_barcode_label),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.barcode_scanning_hardware_scanner_info_usage),
                textAlign = TextAlign.Center,
            )
            WCColoredButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.barcode_scanning_hardware_scanner_info_link),
                onClick = onHardwareScannerInfoClicked,
            )
        }
    }
    if (isHardwareScannerInfoVisible.value) {
        HardwareBarcodeScannerInfoSheet(
            onTestScanSucceeded = onHardwareScannerTestScanSucceeded,
            onTestScanFailed = onHardwareScannerTestScanFailed,
            onDismissRequest = onHardwareScannerInfoDismissed,
        )
    }
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
