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
                host = uiState.host,
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
                onPrimaryClicked = if (uiState.retryTicket != null) onRetryExchange else onStartOver,
                onSecondaryClicked = onFallbackClicked,
            )
            is UiState.Authenticating -> QrLoginAuthenticatingScreen()
            UiState.Idle -> Unit
        }
    }
}

data class QrLoginErrorContent(
    @StringRes val title: Int,
    @StringRes val body: Int,
    @StringRes val primaryAction: Int,
    @StringRes val secondaryAction: Int = R.string.login_qr_endpoint_missing_enter_url,
    val bodyHighlightedArgs: List<Int> = emptyList(),
)

private fun ErrorReason.toErrorContent(): QrLoginErrorContent =
    // InstallQrCode is the only reason that needs the bodyHighlightedArgs field, so it has a
    // dedicated builder. Everything else is a simple title/body/primaryAction triple, so
    // the lookup table keeps this function flat and lets us add new reasons without
    // expanding the cyclomatic complexity of a giant `when` expression.
    if (this == ErrorReason.InstallQrCode) installQrErrorContent() else simpleErrorContents.getValue(this)

private val simpleErrorContents: Map<ErrorReason, QrLoginErrorContent> = mapOf(
    ErrorReason.InvalidPayload to errorContent(
        R.string.login_qr_scanner_error_payload_title,
        R.string.login_qr_scanner_error_payload_body,
        R.string.login_qr_error_primary_scan,
    ),
    ErrorReason.TokenRejected to errorContent(
        R.string.login_qr_scanner_error_token_title,
        R.string.login_qr_scanner_error_token_body,
        R.string.login_qr_error_primary_scan,
    ),
    ErrorReason.EndpointMissing to errorContent(
        R.string.login_qr_endpoint_missing_title,
        R.string.login_qr_endpoint_missing_body,
        R.string.login_qr_endpoint_missing_retry,
    ),
    ErrorReason.RateLimited to errorContent(
        R.string.login_qr_scanner_error_rate_limited_title,
        R.string.login_qr_scanner_error_rate_limited_body,
        R.string.login_qr_error_primary_retry,
    ),
    ErrorReason.Network to errorContent(
        R.string.login_qr_scanner_error_network_title,
        R.string.login_qr_scanner_error_network_body,
        R.string.login_qr_error_primary_retry,
    ),
    ErrorReason.ServerError to errorContent(
        R.string.login_qr_scanner_error_server_title,
        R.string.login_qr_scanner_error_server_body,
        R.string.login_qr_error_primary_retry,
    ),
    ErrorReason.SiteAuthFailure to errorContent(
        R.string.login_qr_scanner_error_site_auth_title,
        R.string.login_qr_scanner_error_site_auth_body,
        R.string.login_qr_error_primary_retry,
    ),
    ErrorReason.NotAWooSite to errorContent(
        R.string.login_qr_scanner_error_not_woo_title,
        R.string.login_qr_scanner_error_not_woo_body,
        R.string.login_qr_error_primary_scan,
    ),
    ErrorReason.UserNotEligible to errorContent(
        R.string.login_qr_scanner_error_user_role_title,
        R.string.login_qr_scanner_error_user_role_body,
        R.string.login_qr_error_primary_retry,
    ),
    ErrorReason.MatchRejected to errorContent(
        R.string.login_qr_scanner_error_match_rejected_title,
        R.string.login_qr_scanner_error_match_rejected_body,
        R.string.login_qr_error_primary_scan,
    ),
    ErrorReason.MatchTimedOut to errorContent(
        R.string.login_qr_scanner_error_match_timed_out_title,
        R.string.login_qr_scanner_error_match_timed_out_body,
        R.string.login_qr_error_primary_scan,
    ),
    ErrorReason.MatchAlreadyScanned to errorContent(
        R.string.login_qr_scanner_error_match_already_scanned_title,
        R.string.login_qr_scanner_error_match_already_scanned_body,
        R.string.login_qr_error_primary_scan,
    ),
    ErrorReason.MatchInvalidGrant to errorContent(
        R.string.login_qr_scanner_error_match_invalid_grant_title,
        R.string.login_qr_scanner_error_match_invalid_grant_body,
        R.string.login_qr_error_primary_scan,
    ),
    ErrorReason.Scanner to genericErrorContent(),
    ErrorReason.Unknown to genericErrorContent(),
)

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
