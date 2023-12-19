package com.woocommerce.android.ui.products.inventory

import androidx.camera.core.ImageProxy
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.barcodescanner.BarcodeScannerScreen
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel
import com.woocommerce.android.ui.compose.component.ProgressIndicator
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterialApi::class)
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
    onManualQuantityEntered: (String) -> Unit
) = WooThemeWithBackground {
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    ModalBottomSheetLayout(
        sheetState = sheetState,
        content = {
            BarcodeScannerScreen(
                onNewFrame = onNewFrame,
                onBindingException = onBindingException,
                permissionState = permissionState,
                onResult = onCameraPermissionResult,
            )
            if (viewState.value is ScanToUpdateInventoryViewModel.ViewState.Loading) {
                ProgressIndicator(backgroundColor = colorResource(id = R.color.color_scrim_background))
            }
        },
        sheetShape = RoundedCornerShape(
            topStart = dimensionResource(id = R.dimen.corner_radius_large),
            topEnd = dimensionResource(id = R.dimen.corner_radius_large)
        ),
        sheetContent = {
            viewState.value.let { state ->
                if (state is ScanToUpdateInventoryViewModel.ViewState.QuickInventoryBottomSheetVisible) {
                    QuickInventoryUpdateBottomSheet(
                        state = state,
                        onIncrementQuantityClicked = onIncrementQuantityClicked,
                        onManualQuantityEntered = onManualQuantityEntered,
                        onUpdateQuantityClicked = onUpdateQuantityClicked,
                        onViewProductDetailsClicked = onViewProductDetailsClicked,
                    )
                }
                LaunchedEffect(state) {
                    if (state is ScanToUpdateInventoryViewModel.ViewState.QuickInventoryBottomSheetVisible) {
                        sheetState.show()
                    } else {
                        sheetState.hide()
                    }
                }
                LaunchedEffect(sheetState) {
                    snapshotFlow { sheetState.currentValue }
                        .filter { it == ModalBottomSheetValue.Hidden }
                        .collect {
                            onBottomSheetDismissed()
                        }
                }
            }
        }
    )
}
