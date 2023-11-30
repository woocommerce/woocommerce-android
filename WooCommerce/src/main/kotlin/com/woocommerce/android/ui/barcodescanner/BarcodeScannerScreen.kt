package com.woocommerce.android.ui.barcodescanner

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.creation.CodeScanner
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import kotlinx.coroutines.flow.Flow

@Composable
fun BarcodeScannerScreen(
    codeScanner: CodeScanner,
    permissionState: BarcodeScanningViewModel.PermissionState,
    onResult: (Boolean) -> Unit,
    onScannedResult: (Flow<CodeScannerStatus>) -> Unit,
    isContinuousScanningEnabled: Boolean = false,
    overlay: @Composable BoxScope.() -> Unit,
) {
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            onResult(granted)
        },
    )
    LaunchedEffect(key1 = Unit) {
        cameraPermissionLauncher.launch(BarcodeScanningFragment.KEY_CAMERA_PERMISSION)
    }
    Box {
        when (permissionState) {
            BarcodeScanningViewModel.PermissionState.Granted -> {
                BarcodeScanner(
                    codeScanner = codeScanner,
                    onScannedResult = onScannedResult,
                    isContinuousScanningEnabled = isContinuousScanningEnabled,
                )
            }
            is BarcodeScanningViewModel.PermissionState.ShouldShowRationale -> {
                AlertDialog(
                    title = stringResource(id = permissionState.title),
                    message = stringResource(id = permissionState.message),
                    ctaLabel = stringResource(id = permissionState.ctaLabel),
                    dismissCtaLabel = stringResource(id = permissionState.dismissCtaLabel),
                    ctaAction = { permissionState.ctaAction.invoke(cameraPermissionLauncher) },
                    dismissCtaAction = { permissionState.dismissCtaAction.invoke() }
                )
            }
            is BarcodeScanningViewModel.PermissionState.PermanentlyDenied -> {
                AlertDialog(
                    title = stringResource(id = permissionState.title),
                    message = stringResource(id = permissionState.message),
                    ctaLabel = stringResource(id = permissionState.ctaLabel),
                    dismissCtaLabel = stringResource(id = permissionState.dismissCtaLabel),
                    ctaAction = { permissionState.ctaAction.invoke(cameraPermissionLauncher) },
                    dismissCtaAction = { permissionState.dismissCtaAction.invoke() }
                )
            }
            BarcodeScanningViewModel.PermissionState.Unknown -> {
                // no-op
            }
        }
        overlay()
    }
}

@Composable
private fun AlertDialog(
    title: String,
    message: String,
    ctaLabel: String,
    dismissCtaLabel: String,
    ctaAction: () -> Unit,
    dismissCtaAction: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { dismissCtaAction() },
        title = {
            Text(title)
        },
        text = {
            Text(message)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    ctaAction()
                }
            ) {
                Text(
                    ctaLabel,
                    color = MaterialTheme.colors.secondary,
                    modifier = Modifier.padding(8.dp)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    dismissCtaAction()
                }
            ) {
                Text(
                    dismissCtaLabel,
                    color = MaterialTheme.colors.secondary,
                    modifier = Modifier.padding(8.dp)
                )
            }
        },
    )
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DeniedOnceAlertDialog() {
    WooThemeWithBackground {
        AlertDialog(
            title = stringResource(id = R.string.barcode_scanning_alert_dialog_title),
            message = stringResource(id = R.string.barcode_scanning_alert_dialog_rationale_message),
            ctaLabel = stringResource(id = R.string.barcode_scanning_alert_dialog_rationale_cta_label),
            dismissCtaLabel = stringResource(id = R.string.barcode_scanning_alert_dialog_dismiss_label),
            ctaAction = {},
            dismissCtaAction = {},
        )
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DeniedPermanentlyAlertDialog() {
    WooThemeWithBackground {
        AlertDialog(
            title = stringResource(id = R.string.barcode_scanning_alert_dialog_title),
            message = stringResource(id = R.string.barcode_scanning_alert_dialog_permanently_denied_message),
            ctaLabel = stringResource(id = R.string.barcode_scanning_alert_dialog_permanently_denied_cta_label),
            dismissCtaLabel = stringResource(id = R.string.barcode_scanning_alert_dialog_dismiss_label),
            ctaAction = {},
            dismissCtaAction = {},
        )
    }
}
