package com.woocommerce.android.ui.login.qrlogin

import androidx.annotation.StringRes
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.woocommerce.android.R
import com.woocommerce.android.ui.barcodescanner.BarcodeScannerScreen
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel
import com.woocommerce.android.ui.login.HelpButton
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState
import com.woocommerce.android.ui.login.qrlogin.flow.ErrorReason

/**
 * Renders the QR-first login screen. Routes between the camera scanner, the
 * number-matching approval step (post-scan, awaiting the merchant's tap on wc-admin),
 * the fullscreen error (which subsumes the endpoint-missing fallback), and the fullscreen
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
    onCancelNumberMatch: () -> Unit,
    onConfirmSessionReplace: () -> Unit,
    onCancelSessionReplace: () -> Unit,
    onStartOver: () -> Unit,
    onRetryExchange: () -> Unit,
    onFallbackClicked: () -> Unit,
    onHelpClicked: () -> Unit,
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
            is UiState.WaitingForApproval -> QrLoginNumberDisplayScreen(
                subtitle = uiState.subtitle,
                realNumber = uiState.realNumber,
                expiresAtEpochMs = uiState.expiresAtEpochMs,
                onCancel = onCancelNumberMatch,
            )
            is UiState.WarningSessionReplace -> QrLoginSessionReplaceWarningScreen(
                onConfirm = onConfirmSessionReplace,
                onCancel = onCancelSessionReplace,
            )
            is UiState.Error -> QrLoginErrorScreen(
                content = uiState.reason.toErrorContent(),
                onPrimaryClicked = if (uiState.retryable) onRetryExchange else onStartOver,
                onSecondaryClicked = onFallbackClicked,
            )
            is UiState.Authenticating -> QrLoginAuthenticatingScreen()
            UiState.Idle -> Unit
        }

        // White tint when the camera preview is showing behind the icon, otherwise default
        // onSurface so the icon stays readable on the opaque confirm/error/authenticating overlays.
        val helpTint = if (showCamera && uiState == UiState.Idle) {
            Color.White
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        HelpButton(
            onClick = onHelpClicked,
            tint = helpTint,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

data class QrLoginErrorContent(
    @StringRes val title: Int,
    @StringRes val body: Int,
    @StringRes val primaryAction: Int,
    @StringRes val secondaryAction: Int = R.string.login_qr_endpoint_missing_enter_url,
    val bodyHighlightedArgs: List<Int> = emptyList(),
)

/**
 * Exhaustive `when` over the sealed [ErrorReason] hierarchy — adding a new variant becomes a
 * compile error rather than a runtime crash, in contrast with the `Map.getValue()` lookup the
 * earlier enum-based implementation used.
 */
private fun ErrorReason.toErrorContent(): QrLoginErrorContent = when (this) {
    ErrorReason.InstallQrCode -> installQrErrorContent()
    ErrorReason.InvalidPayload -> errorContent(
        R.string.login_qr_scanner_error_payload_title,
        R.string.login_qr_scanner_error_payload_body,
        R.string.login_qr_error_primary_scan,
    )
    ErrorReason.TokenRejected -> errorContent(
        R.string.login_qr_scanner_error_token_title,
        R.string.login_qr_scanner_error_token_body,
        R.string.login_qr_error_primary_scan,
    )
    ErrorReason.EndpointMissing -> errorContent(
        R.string.login_qr_endpoint_missing_title,
        R.string.login_qr_endpoint_missing_body,
        R.string.login_qr_endpoint_missing_retry,
    )
    ErrorReason.RateLimited -> errorContent(
        R.string.login_qr_scanner_error_rate_limited_title,
        R.string.login_qr_scanner_error_rate_limited_body,
        R.string.login_qr_error_primary_retry,
    )
    ErrorReason.Network -> errorContent(
        R.string.login_qr_scanner_error_network_title,
        R.string.login_qr_scanner_error_network_body,
        R.string.login_qr_error_primary_retry,
    )
    ErrorReason.ServerError -> errorContent(
        R.string.login_qr_scanner_error_server_title,
        R.string.login_qr_scanner_error_server_body,
        R.string.login_qr_error_primary_retry,
    )
    ErrorReason.SiteAuthFailure -> errorContent(
        R.string.login_qr_scanner_error_site_auth_title,
        R.string.login_qr_scanner_error_site_auth_body,
        R.string.login_qr_error_primary_retry,
    )
    ErrorReason.NotAWooSite -> errorContent(
        R.string.login_qr_scanner_error_not_woo_title,
        R.string.login_qr_scanner_error_not_woo_body,
        R.string.login_qr_error_primary_scan,
    )
    ErrorReason.UserNotEligible -> errorContent(
        R.string.login_qr_scanner_error_user_role_title,
        R.string.login_qr_scanner_error_user_role_body,
        R.string.login_qr_error_primary_retry,
    )
    ErrorReason.MatchRejected -> errorContent(
        R.string.login_qr_scanner_error_match_rejected_title,
        R.string.login_qr_scanner_error_match_rejected_body,
        R.string.login_qr_error_primary_scan,
    )
    ErrorReason.MatchTimedOut -> errorContent(
        R.string.login_qr_scanner_error_match_timed_out_title,
        R.string.login_qr_scanner_error_match_timed_out_body,
        R.string.login_qr_error_primary_scan,
    )
    ErrorReason.MatchAlreadyScanned -> errorContent(
        R.string.login_qr_scanner_error_match_already_scanned_title,
        R.string.login_qr_scanner_error_match_already_scanned_body,
        R.string.login_qr_error_primary_scan,
    )
    ErrorReason.MatchInvalidGrant -> errorContent(
        R.string.login_qr_scanner_error_match_invalid_grant_title,
        R.string.login_qr_scanner_error_match_invalid_grant_body,
        R.string.login_qr_error_primary_scan,
    )
    ErrorReason.MatchAlreadyCompleted -> errorContent(
        R.string.login_qr_scanner_error_match_already_completed_title,
        R.string.login_qr_scanner_error_match_already_completed_body,
        R.string.login_qr_error_primary_scan,
    )
    ErrorReason.Scanner,
    ErrorReason.Unknown -> genericErrorContent()
}

private fun errorContent(
    @StringRes title: Int,
    @StringRes body: Int,
    @StringRes primaryAction: Int,
): QrLoginErrorContent = QrLoginErrorContent(title = title, body = body, primaryAction = primaryAction)

private fun genericErrorContent() = errorContent(
    title = R.string.login_qr_scanner_error_generic_title,
    body = R.string.login_qr_scanner_error_generic_body,
    primaryAction = R.string.login_qr_error_primary_retry,
)

private fun installQrErrorContent() = QrLoginErrorContent(
    title = R.string.login_qr_scanner_error_install_qr_title,
    body = R.string.login_qr_scanner_error_install_qr_body,
    primaryAction = R.string.login_qr_error_primary_scan,
    bodyHighlightedArgs = listOf(
        R.string.login_qr_scanner_error_install_qr_body_button,
        R.string.login_qr_prologue_url,
    ),
)
