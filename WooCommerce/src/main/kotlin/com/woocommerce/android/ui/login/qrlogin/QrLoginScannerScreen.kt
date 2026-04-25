package com.woocommerce.android.ui.login.qrlogin

import androidx.camera.core.ImageProxy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.woocommerce.android.R
import com.woocommerce.android.ui.barcodescanner.BarcodeScannerScreen
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.ErrorReason
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState

/**
 * Renders the QR-first login screen. Routes between the camera scanner, fullscreen confirm,
 * the endpoint-missing fallback, and the signing-in progress dialog. The fragment plumbs camera
 * permissions and the post-login handoff; this composable is purely UI routing.
 */
@Composable
fun QrLoginScannerScreen(
    permissionState: State<BarcodeScanningViewModel.PermissionState>,
    uiState: UiState,
    showCamera: Boolean,
    onNewFrame: (ImageProxy) -> Unit,
    onBindingException: (Exception) -> Unit,
    onPermissionResult: (Boolean) -> Unit,
    onConfirmSite: () -> Unit,
    onCancelSite: () -> Unit,
    onStartOver: () -> Unit,
    onFallbackClicked: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (showCamera) {
            BarcodeScannerScreen(
                onNewFrame = onNewFrame,
                onBindingException = onBindingException,
                permissionState = permissionState,
                onResult = onPermissionResult,
                overlayLabel = R.string.login_qr_scanner_hint,
                overlayContent = { QrLoginViewfinder() }
            )
        }

        when (uiState) {
            is UiState.Confirming -> QrLoginConfirmSiteScreen(
                host = uiState.host,
                onConfirm = onConfirmSite,
                onCancel = onCancelSite,
            )
            is UiState.Error -> if (uiState.reason == ErrorReason.EndpointMissing) {
                QrLoginEndpointMissingScreen(
                    onEnterUrlClicked = onFallbackClicked,
                    onRetryClicked = onStartOver,
                )
            }
            UiState.Authenticating -> ProgressDialog(
                title = "",
                subtitle = stringResource(id = R.string.login_qr_scanner_authenticating),
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            )
            UiState.Idle -> Unit
        }
    }
}
