package com.woocommerce.android.ui.woopos.settings.details.hardware.barcodescanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.util.ScannerInfo
import com.woocommerce.android.ui.woopos.common.util.ScannerType
import com.woocommerce.android.ui.woopos.settings.details.WooPosSettingsDetailsMenuItem
import com.woocommerce.android.util.ChromeCustomTabUtils
import kotlinx.coroutines.flow.collectLatest

@Composable
fun WooPosSettingsHardwareBarcodeScannerScreen(
    onShowScanningSetupDialog: () -> Unit,
    viewModel: WooPosSettingsHardwareBarcodeScannerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.showScanningSetupDialog.collectLatest {
            onShowScanningSetupDialog()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.openUrl.collectLatest { url ->
            ChromeCustomTabUtils.launchUrl(context, url, enableSlideAnimation = true)
        }
    }

    WooPosSettingsHardwareBarcodeScannerContent(
        scannerInfo = state.scannerInfo,
        onSetupScannerClicked = { viewModel.onSetupScannerClicked() },
        onDocumentationClicked = { viewModel.onDocumentationClicked() }
    )
}

@Composable
fun WooPosSettingsHardwareBarcodeScannerContent(
    scannerInfo: ScannerInfo,
    onSetupScannerClicked: () -> Unit,
    onDocumentationClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        when (scannerInfo) {
            is ScannerInfo.Connected -> {
                ConnectedScannerSection(scannerInfo = scannerInfo)
            }
            else -> {}
        }

        WooPosSettingsDetailsMenuItem(
            icon = Icons.Default.Settings,
            title = stringResource(R.string.woopos_settings_barcode_scanner_setup_title),
            subtitle = stringResource(R.string.woopos_settings_barcode_scanner_setup_subtitle),
            onClick = onSetupScannerClicked
        )

        WooPosSettingsDetailsMenuItem(
            icon = Icons.Default.Description,
            title = stringResource(R.string.woopos_settings_barcode_scanner_documentation_title),
            subtitle = stringResource(R.string.woopos_settings_barcode_scanner_documentation_subtitle),
            onClick = onDocumentationClicked
        )
    }
}

@Composable
private fun ConnectedScannerSection(scannerInfo: ScannerInfo.Connected) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WooPosSpacing.Medium.value)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(WooPosCornerRadius.Medium.value)
            )
            .padding(WooPosSpacing.Large.value)
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_settings_barcode_scanner_connected),
            style = WooPosTypography.BodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value)
        ) {
            Icon(
                imageVector = when (scannerInfo.type) {
                    ScannerType.BLUETOOTH -> Icons.Default.Bluetooth
                    ScannerType.USB_HID -> Icons.Default.Usb
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Column {
                WooPosText(
                    text = scannerInfo.name,
                    style = WooPosTypography.BodyMedium,
                    fontWeight = FontWeight.Medium
                )
                WooPosText(
                    text = when (scannerInfo.type) {
                        ScannerType.BLUETOOTH -> stringResource(R.string.woopos_settings_barcode_scanner_bluetooth)
                        ScannerType.USB_HID -> stringResource(R.string.woopos_settings_barcode_scanner_usb)
                    },
                    style = WooPosTypography.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsHardwareBarcodeScannerScreenWithConnectedScannerPreview() {
    WooPosTheme {
        WooPosSettingsHardwareBarcodeScannerContent(
            scannerInfo = ScannerInfo.Connected(
                name = "Socket Mobile S700",
                type = ScannerType.BLUETOOTH
            ),
            onSetupScannerClicked = { },
            onDocumentationClicked = { }
        )
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsHardwareBarcodeScannerScreenWithoutScannerPreview() {
    WooPosTheme {
        WooPosSettingsHardwareBarcodeScannerContent(
            scannerInfo = ScannerInfo.NoScannerDetected,
            onSetupScannerClicked = { },
            onDocumentationClicked = { }
        )
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsHardwareBarcodeScannerScreenWithUsbScannerPreview() {
    WooPosTheme {
        WooPosSettingsHardwareBarcodeScannerContent(
            scannerInfo = ScannerInfo.Connected(
                name = "Zebra LI3608-ER",
                type = ScannerType.USB_HID
            ),
            onSetupScannerClicked = { },
            onDocumentationClicked = { }
        )
    }
}
