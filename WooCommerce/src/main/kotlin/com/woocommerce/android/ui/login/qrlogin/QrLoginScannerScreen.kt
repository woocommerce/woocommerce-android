package com.woocommerce.android.ui.login.qrlogin

import androidx.annotation.StringRes
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import com.woocommerce.android.R
import com.woocommerce.android.ui.barcodescanner.BarcodeScannerScreen
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.ErrorReason
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState

/**
 * Renders the QR-first login screen. Routes between the camera scanner, fullscreen confirm,
 * fullscreen error (which subsumes the endpoint-missing fallback), and the fullscreen
 * signing-in state. The fragment plumbs camera permissions and the post-login handoff; this
 * composable is purely UI routing.
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
    onRetryExchange: () -> Unit,
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
            is UiState.Error -> QrLoginErrorScreen(
                content = uiState.reason.toErrorContent(),
                onPrimaryClicked = if (uiState.retryTicket != null) onRetryExchange else onStartOver,
                onSecondaryClicked = onFallbackClicked,
            )
            UiState.Authenticating -> QrLoginAuthenticatingScreen()
            UiState.Idle -> Unit
        }
    }
}

data class QrLoginErrorContent(
    @StringRes val title: Int,
    @StringRes val body: Int,
    @StringRes val primaryAction: Int,
    @StringRes val secondaryAction: Int = R.string.login_qr_endpoint_missing_enter_url,
    val bodyArgs: List<Int> = emptyList(),
)

private fun ErrorReason.toErrorContent(): QrLoginErrorContent = when (this) {
    ErrorReason.InvalidPayload -> QrLoginErrorContent(
        title = R.string.login_qr_scanner_error_payload_title,
        body = R.string.login_qr_scanner_error_payload_body,
        primaryAction = R.string.login_qr_error_primary_scan,
    )
    ErrorReason.InstallQrCode -> installQrErrorContent()
    ErrorReason.TokenRejected -> QrLoginErrorContent(
        title = R.string.login_qr_scanner_error_token_title,
        body = R.string.login_qr_scanner_error_token_body,
        primaryAction = R.string.login_qr_error_primary_scan,
    )
    ErrorReason.EndpointMissing -> QrLoginErrorContent(
        title = R.string.login_qr_endpoint_missing_title,
        body = R.string.login_qr_endpoint_missing_body,
        primaryAction = R.string.login_qr_endpoint_missing_retry,
    )
    ErrorReason.RateLimited -> QrLoginErrorContent(
        title = R.string.login_qr_scanner_error_rate_limited_title,
        body = R.string.login_qr_scanner_error_rate_limited_body,
        primaryAction = R.string.login_qr_error_primary_retry,
    )
    ErrorReason.Network -> QrLoginErrorContent(
        title = R.string.login_qr_scanner_error_network_title,
        body = R.string.login_qr_scanner_error_network_body,
        primaryAction = R.string.login_qr_error_primary_retry,
    )
    ErrorReason.ServerError -> QrLoginErrorContent(
        title = R.string.login_qr_scanner_error_server_title,
        body = R.string.login_qr_scanner_error_server_body,
        primaryAction = R.string.login_qr_error_primary_retry,
    )
    ErrorReason.SiteAuthFailure -> QrLoginErrorContent(
        title = R.string.login_qr_scanner_error_site_auth_title,
        body = R.string.login_qr_scanner_error_site_auth_body,
        primaryAction = R.string.login_qr_error_primary_retry,
    )
    ErrorReason.NotAWooSite -> QrLoginErrorContent(
        title = R.string.login_qr_scanner_error_not_woo_title,
        body = R.string.login_qr_scanner_error_not_woo_body,
        primaryAction = R.string.login_qr_error_primary_scan,
    )
    ErrorReason.UserNotEligible -> QrLoginErrorContent(
        title = R.string.login_qr_scanner_error_user_role_title,
        body = R.string.login_qr_scanner_error_user_role_body,
        primaryAction = R.string.login_qr_error_primary_retry,
    )
    ErrorReason.Scanner,
    ErrorReason.Unknown -> QrLoginErrorContent(
        title = R.string.login_qr_scanner_error_generic_title,
        body = R.string.login_qr_scanner_error_generic_body,
        primaryAction = R.string.login_qr_error_primary_retry,
    )
}

private fun installQrErrorContent() = QrLoginErrorContent(
    title = R.string.login_qr_scanner_error_install_qr_title,
    body = R.string.login_qr_scanner_error_install_qr_body,
    primaryAction = R.string.login_qr_error_primary_scan,
    bodyArgs = listOf(
        R.string.login_qr_scanner_error_install_qr_body_button,
        R.string.login_qr_prologue_url,
    ),
)
