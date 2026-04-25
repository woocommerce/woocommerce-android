package com.woocommerce.android.ui.login.qrlogin

import androidx.annotation.StringRes
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.woocommerce.android.R
import com.woocommerce.android.ui.barcodescanner.BarcodeScannerScreen
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel
import com.woocommerce.android.ui.compose.component.ProgressDialog

/**
 * Renders the QR-first login screen. Three blocking states (endpoint-missing screen, progress
 * dialog, confirm-site dialog) overlay the scanner — or skip it entirely when entered via deep
 * link. The fragment is responsible for plumbing camera permissions, lifecycle events, and the
 * post-login handoff; this composable is purely UI routing.
 */
@Composable
fun QrLoginScannerScreen(
    permissionState: State<BarcodeScanningViewModel.PermissionState>,
    authenticating: Boolean,
    endpointMissing: Boolean,
    pendingConfirmation: QrLoginScannerViewModel.PendingConfirmation?,
    showCamera: Boolean,
    onNewFrame: (ImageProxy) -> Unit,
    onBindingException: (Exception) -> Unit,
    onPermissionResult: (Boolean) -> Unit,
    onConfirmSite: () -> Unit,
    onCancelSite: () -> Unit,
    onStartOver: () -> Unit,
    onFallbackClicked: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (endpointMissing) {
            QrLoginEndpointMissingScreen(
                onEnterUrlClicked = onFallbackClicked,
                onRetryClicked = onStartOver
            )
            return@Box
        }
        if (showCamera) {
            BarcodeScannerScreen(
                onNewFrame = onNewFrame,
                onBindingException = onBindingException,
                permissionState = permissionState,
                onResult = onPermissionResult,
                overlayLabel = R.string.login_qr_scanner_hint
            )
        }
        if (authenticating) {
            ProgressDialog(
                title = "",
                subtitle = stringResource(id = R.string.login_qr_scanner_authenticating),
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            )
        }
        pendingConfirmation?.let { pending ->
            QrLoginConfirmSiteDialog(
                host = pending.host,
                onConfirm = onConfirmSite,
                onCancel = onCancelSite
            )
        }
    }
}

@StringRes
internal fun QrLoginScannerViewModel.ErrorReason.toMessage(): Int = when (this) {
    QrLoginScannerViewModel.ErrorReason.InvalidPayload -> R.string.login_qr_scanner_error_payload
    QrLoginScannerViewModel.ErrorReason.Scanner -> R.string.login_qr_scanner_error_generic
    QrLoginScannerViewModel.ErrorReason.TokenRejected -> R.string.login_qr_scanner_error_token
    QrLoginScannerViewModel.ErrorReason.EndpointMissing -> R.string.login_qr_scanner_error_endpoint
    QrLoginScannerViewModel.ErrorReason.RateLimited -> R.string.login_qr_scanner_error_rate_limited
    QrLoginScannerViewModel.ErrorReason.Network -> R.string.login_qr_scanner_error_network
    QrLoginScannerViewModel.ErrorReason.ServerError -> R.string.login_qr_scanner_error_server
    QrLoginScannerViewModel.ErrorReason.SiteAuthFailure -> R.string.login_qr_scanner_error_site_auth
    QrLoginScannerViewModel.ErrorReason.NotAWooSite -> R.string.login_qr_scanner_error_not_woo
    QrLoginScannerViewModel.ErrorReason.UserNotEligible -> R.string.login_qr_scanner_error_user_role
    QrLoginScannerViewModel.ErrorReason.Unknown -> R.string.login_qr_scanner_error_generic
}
