package com.woocommerce.android.ui.woopos.settings.details.hardware.barcodescanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupDialog
import com.woocommerce.android.ui.woopos.settings.details.WooPosSettingsDetailsMenuItem
import com.woocommerce.android.util.ChromeCustomTabUtils
import kotlinx.coroutines.flow.collectLatest

@Composable
fun WooPosSettingsHardwareBarcodeScannerScreen(
    viewModel: WooPosSettingsHardwareBarcodeScannerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showScanningSetupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.showScanningSetupDialog.collectLatest {
            showScanningSetupDialog = true
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

    WooPosScanningSetupDialog(
        isVisible = showScanningSetupDialog,
        onDismissRequest = { showScanningSetupDialog = false }
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
            .padding(WooPosSpacing.Medium.value)
    ) {
        when (scannerInfo) {
            is ScannerInfo.Connected -> {
                ConnectedScannerSection(scannerInfo = scannerInfo)
                Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
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
            .background(
                color = WooPosTheme.colors.success.copy(alpha = 0.1f),
                shape = RoundedCornerShape(WooPosCornerRadius.Medium.value)
            )
            .border(
                width = 1.dp,
                color = WooPosTheme.colors.success,
                shape = RoundedCornerShape(WooPosCornerRadius.Medium.value)
            )
            .padding(WooPosSpacing.Large.value)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = WooPosTheme.colors.success,
                modifier = Modifier.size(24.dp)
            )
            WooPosText(
                text = stringResource(R.string.woopos_settings_barcode_scanner_connected),
                style = WooPosTypography.BodyLarge,
                fontWeight = FontWeight.Bold,
                color = WooPosTheme.colors.success
            )
        }

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

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
            Column(modifier = Modifier.weight(1f)) {
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
            scannerInfo.batteryLevel?.let { batteryLevel ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = getBatteryIcon(batteryLevel),
                        contentDescription = null,
                        tint = getBatteryColor(batteryLevel),
                        modifier = Modifier.size(20.dp)
                    )
                    WooPosText(
                        text = "$batteryLevel%",
                        style = WooPosTypography.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


@Composable
private fun getBatteryIcon(batteryLevel: Int): ImageVector {
    return when {
        batteryLevel >= 95 -> Icons.Default.BatteryFull
        batteryLevel >= 85 -> Icons.Default.Battery6Bar
        batteryLevel >= 70 -> Icons.Default.Battery5Bar
        batteryLevel >= 55 -> Icons.Default.Battery4Bar
        batteryLevel >= 40 -> Icons.Default.Battery3Bar
        batteryLevel >= 25 -> Icons.Default.Battery2Bar
        batteryLevel >= 10 -> Icons.Default.Battery1Bar
        else -> Icons.Default.Battery0Bar
    }
}

@Composable
private fun getBatteryColor(batteryLevel: Int): Color {
    return when {
        batteryLevel <= 20 -> Color(0xFFD32F2F)
        batteryLevel <= 40 -> Color(0xFFF57C00)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsHardwareBarcodeScannerScreenWithConnectedScannerPreview() {
    WooPosTheme {
        WooPosSettingsHardwareBarcodeScannerContent(
            scannerInfo = ScannerInfo.Connected(
                name = "Socket Mobile S700",
                type = ScannerType.BLUETOOTH,
                batteryLevel = 85
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
fun WooPosSettingsHardwareBarcodeScannerScreenWithLowBatteryPreview() {
    WooPosTheme {
        WooPosSettingsHardwareBarcodeScannerContent(
            scannerInfo = ScannerInfo.Connected(
                name = "Zebra LI3608-ER",
                type = ScannerType.BLUETOOTH,
                batteryLevel = 15
            ),
            onSetupScannerClicked = { },
            onDocumentationClicked = { }
        )
    }
}