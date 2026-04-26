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

/**
 * Renders the QR-first login screen. Routes between the camera scanner, fullscreen confirm,
 * fullscreen error, endpoint-missing fallback, and the fullscreen signing-in state. The fragment
 * plumbs camera permissions and the post-login handoff; this composable is purely UI routing.
 */
@Composable
fun QrLoginScannerScreen(
    permissionState: State<BarcodeScanningViewModel.PermissionState>,
    authenticating: Boolean,
    endpointMissing: Boolean,
    pendingConfirmation: QrLoginScannerViewModel.PendingConfirmation?,
    currentError: QrLoginScannerViewModel.ErrorReason?,
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
        // Camera/scanner is the lowest layer. We always render either the camera (when allowed)
        // or a non-black background so deep-link entry / dismissed errors never reveal black.
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

        // Routing: authenticating is fullscreen and trumps everything; otherwise prefer
        // terminal screens (endpoint missing, error, confirm).
        when {
            authenticating -> QrLoginAuthenticatingScreen()
            endpointMissing -> QrLoginEndpointMissingScreen(
                onEnterUrlClicked = onFallbackClicked,
                onRetryClicked = onStartOver,
            )
            currentError != null -> QrLoginErrorScreen(
                content = currentError.toErrorContent(),
                onPrimaryClicked = if (currentError.isRetryEligible()) onRetryExchange else onStartOver,
                onSecondaryClicked = onFallbackClicked,
            )
            pendingConfirmation != null -> QrLoginConfirmSiteScreen(
                host = pendingConfirmation.host,
                onConfirm = onConfirmSite,
                onCancel = onCancelSite,
            )
        }
    }
}

data class QrLoginErrorContent(
    @StringRes val title: Int,
    @StringRes val body: Int,
    @StringRes val primaryAction: Int,
    @StringRes val secondaryAction: Int = R.string.login_qr_endpoint_missing_enter_url,
)

/**
 * Errors where retrying the exchange with the same token is likely to help. Network failures
 * may not have reached the server, server-side 5xx are typically transient, and rate-limit
 * windows are short. The other reasons (token rejected, payload invalid, not a Woo site,
 * etc.) won't be fixed by retrying — those keep the "scan a new code" / start-over action.
 */
private fun QrLoginScannerViewModel.ErrorReason.isRetryEligible(): Boolean = when (this) {
    QrLoginScannerViewModel.ErrorReason.Network,
    QrLoginScannerViewModel.ErrorReason.ServerError,
    QrLoginScannerViewModel.ErrorReason.RateLimited -> true
    else -> false
}

private fun QrLoginScannerViewModel.ErrorReason.toErrorContent(): QrLoginErrorContent =
    when (this) {
        QrLoginScannerViewModel.ErrorReason.InvalidPayload -> QrLoginErrorContent(
            title = R.string.login_qr_scanner_error_payload_title,
            body = R.string.login_qr_scanner_error_payload_body,
            primaryAction = R.string.login_qr_error_primary_scan,
        )
        QrLoginScannerViewModel.ErrorReason.Scanner -> QrLoginErrorContent(
            title = R.string.login_qr_scanner_error_generic_title,
            body = R.string.login_qr_scanner_error_generic_body,
            primaryAction = R.string.login_qr_error_primary_retry,
        )
        QrLoginScannerViewModel.ErrorReason.TokenRejected -> QrLoginErrorContent(
            title = R.string.login_qr_scanner_error_token_title,
            body = R.string.login_qr_scanner_error_token_body,
            primaryAction = R.string.login_qr_error_primary_scan,
        )
        QrLoginScannerViewModel.ErrorReason.EndpointMissing -> QrLoginErrorContent(
            title = R.string.login_qr_endpoint_missing_title,
            body = R.string.login_qr_endpoint_missing_body,
            primaryAction = R.string.login_qr_endpoint_missing_retry,
        )
        QrLoginScannerViewModel.ErrorReason.RateLimited -> QrLoginErrorContent(
            title = R.string.login_qr_scanner_error_rate_limited_title,
            body = R.string.login_qr_scanner_error_rate_limited_body,
            primaryAction = R.string.login_qr_error_primary_retry,
        )
        QrLoginScannerViewModel.ErrorReason.Network -> QrLoginErrorContent(
            title = R.string.login_qr_scanner_error_network_title,
            body = R.string.login_qr_scanner_error_network_body,
            primaryAction = R.string.login_qr_error_primary_retry,
        )
        QrLoginScannerViewModel.ErrorReason.ServerError -> QrLoginErrorContent(
            title = R.string.login_qr_scanner_error_server_title,
            body = R.string.login_qr_scanner_error_server_body,
            primaryAction = R.string.login_qr_error_primary_retry,
        )
        QrLoginScannerViewModel.ErrorReason.SiteAuthFailure -> QrLoginErrorContent(
            title = R.string.login_qr_scanner_error_site_auth_title,
            body = R.string.login_qr_scanner_error_site_auth_body,
            primaryAction = R.string.login_qr_error_primary_retry,
        )
        QrLoginScannerViewModel.ErrorReason.NotAWooSite -> QrLoginErrorContent(
            title = R.string.login_qr_scanner_error_not_woo_title,
            body = R.string.login_qr_scanner_error_not_woo_body,
            primaryAction = R.string.login_qr_error_primary_scan,
        )
        QrLoginScannerViewModel.ErrorReason.UserNotEligible -> QrLoginErrorContent(
            title = R.string.login_qr_scanner_error_user_role_title,
            body = R.string.login_qr_scanner_error_user_role_body,
            primaryAction = R.string.login_qr_error_primary_retry,
        )
        QrLoginScannerViewModel.ErrorReason.Unknown -> QrLoginErrorContent(
            title = R.string.login_qr_scanner_error_generic_title,
            body = R.string.login_qr_scanner_error_generic_body,
            primaryAction = R.string.login_qr_error_primary_retry,
        )
    }
