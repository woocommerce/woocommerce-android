package com.woocommerce.android.ui.barcodescanner

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.theme.LegacyWooThemeWithBackground

@Composable
fun BarcodeScannerScreen(
    onNewFrame: (ImageProxy) -> Unit,
    onBindingException: (Exception) -> Unit,
    permissionState: State<BarcodeScanningViewModel.PermissionState>,
    onResult: (Boolean) -> Unit,
    overlayContent: @Composable () -> Unit,
    onHardwareScannerInfoClicked: (() -> Unit)?,
) = BarcodeScannerScreen(
    onNewFrame = onNewFrame,
    onBindingException = onBindingException,
    permissionState = permissionState.value,
    onResult = onResult,
    overlayContent = overlayContent,
    onHardwareScannerInfoClicked = onHardwareScannerInfoClicked,
)

@Composable
fun BarcodeScannerScreen(
    onNewFrame: (ImageProxy) -> Unit,
    onBindingException: (Exception) -> Unit,
    permissionState: BarcodeScanningViewModel.PermissionState,
    onResult: (Boolean) -> Unit,
    overlayContent: @Composable () -> Unit,
    onHardwareScannerInfoClicked: (() -> Unit)?,
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
    when (permissionState) {
        BarcodeScanningViewModel.PermissionState.Granted -> {
            BarcodeScanner(
                onNewFrame = onNewFrame,
                onBindingException = onBindingException,
                overlayContent = overlayContent,
            )
        }
        is BarcodeScanningViewModel.PermissionState.ShouldShowRationale -> {
            PermissionDeniedContent(
                title = stringResource(id = permissionState.title),
                message = stringResource(id = permissionState.message),
                ctaLabel = stringResource(id = permissionState.ctaLabel),
                dismissCtaLabel = stringResource(id = permissionState.dismissCtaLabel),
                ctaAction = { permissionState.ctaAction.invoke(cameraPermissionLauncher) },
                dismissCtaAction = { permissionState.dismissCtaAction.invoke() },
                onHardwareScannerInfoClicked = onHardwareScannerInfoClicked,
            )
        }
        is BarcodeScanningViewModel.PermissionState.PermanentlyDenied -> {
            PermissionDeniedContent(
                title = stringResource(id = permissionState.title),
                message = stringResource(id = permissionState.message),
                ctaLabel = stringResource(id = permissionState.ctaLabel),
                dismissCtaLabel = stringResource(id = permissionState.dismissCtaLabel),
                ctaAction = { permissionState.ctaAction.invoke(cameraPermissionLauncher) },
                dismissCtaAction = { permissionState.dismissCtaAction.invoke() },
                onHardwareScannerInfoClicked = onHardwareScannerInfoClicked,
            )
        }
        BarcodeScanningViewModel.PermissionState.Unknown -> {
            // no-op
        }
    }
}

@Composable
private fun PermissionDeniedContent(
    title: String,
    message: String,
    ctaLabel: String,
    dismissCtaLabel: String,
    ctaAction: () -> Unit,
    dismissCtaAction: () -> Unit,
    onHardwareScannerInfoClicked: (() -> Unit)?,
) {
    if (onHardwareScannerInfoClicked == null) {
        PermissionAlertDialog(
            title = title,
            message = message,
            ctaLabel = ctaLabel,
            dismissCtaLabel = dismissCtaLabel,
            ctaAction = ctaAction,
            dismissCtaAction = dismissCtaAction,
        )
    } else {
        CameraPermissionDeniedPanel(
            title = title,
            message = message,
            ctaLabel = ctaLabel,
            ctaAction = ctaAction,
            onHardwareScannerInfoClicked = onHardwareScannerInfoClicked,
        )
    }
}

/**
 * Keeps the external scanner option reachable when the camera can't be used.
 */
@Composable
private fun CameraPermissionDeniedPanel(
    title: String,
    message: String,
    ctaLabel: String,
    ctaAction: () -> Unit,
    onHardwareScannerInfoClicked: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(id = R.dimen.major_100)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = message,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.major_100)),
            )
            WCOutlinedButton(
                onClick = ctaAction,
                text = ctaLabel,
                modifier = Modifier.fillMaxWidth(),
            )
            ScannerOptionsDivider()
            HardwareBarcodeScannerButton(onClick = onHardwareScannerInfoClicked)
        }
    }
}

@Composable
private fun PermissionAlertDialog(
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
                    color = MaterialTheme.colorScheme.secondary,
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
                    color = MaterialTheme.colorScheme.secondary,
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
    LegacyWooThemeWithBackground {
        PermissionAlertDialog(
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
    LegacyWooThemeWithBackground {
        PermissionAlertDialog(
            title = stringResource(id = R.string.barcode_scanning_alert_dialog_title),
            message = stringResource(id = R.string.barcode_scanning_alert_dialog_permanently_denied_message),
            ctaLabel = stringResource(id = R.string.barcode_scanning_alert_dialog_permanently_denied_cta_label),
            dismissCtaLabel = stringResource(id = R.string.barcode_scanning_alert_dialog_dismiss_label),
            ctaAction = {},
            dismissCtaAction = {},
        )
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CameraPermissionDeniedPanelPreview() {
    LegacyWooThemeWithBackground {
        CameraPermissionDeniedPanel(
            title = stringResource(id = R.string.barcode_scanning_alert_dialog_title),
            message = stringResource(id = R.string.barcode_scanning_alert_dialog_permanently_denied_message),
            ctaLabel = stringResource(id = R.string.barcode_scanning_alert_dialog_permanently_denied_cta_label),
            ctaAction = {},
            onHardwareScannerInfoClicked = {},
        )
    }
}
