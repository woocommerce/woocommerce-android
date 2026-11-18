package com.woocommerce.android.ui.woopos.settings.details.hardware.receiptprinter

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButtonSmall
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.settings.details.WooPosSettingsDetailsMenuItem
import com.woocommerce.android.ui.woopos.settings.details.WooPosSettingsDetailsMenuItemInfo

@Composable
fun WooPosSettingsHardwareReceiptPrinterScreen(
    viewModel: WooPosSettingsHardwareReceiptPrinterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    WooPosSettingsHardwareReceiptPrinterContent(
        uiState = uiState,
        onConnectClicked = viewModel::onConnectClicked,
        onDisconnectClicked = viewModel::onDisconnectClicked,
    )
}

@Composable
private fun WooPosSettingsHardwareReceiptPrinterContent(
    uiState: WooPosSettingsHardwareReceiptPrinterUiState,
    onConnectClicked: () -> Unit,
    onDisconnectClicked: () -> Unit,
) {
    AnimatedContent(
        targetState = uiState,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "receipt_printer_state_animation"
    ) { state ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WooPosSpacing.Medium.value),
        ) {
            when (state) {
                is WooPosSettingsHardwareReceiptPrinterUiState.Connected -> {
                    ConnectedContent(
                        printerName = state.printerName,
                        onDisconnectClicked = onDisconnectClicked,
                    )
                }

                is WooPosSettingsHardwareReceiptPrinterUiState.Disconnected -> {
                    WooPosSettingsDetailsMenuItem(
                        title = stringResource(R.string.woopos_settings_hardware_receipt_printers_connect_title),
                        subtitle = stringResource(R.string.woopos_settings_hardware_receipt_printers_connect_subtitle),
                        onClick = onConnectClicked
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectedContent(
    printerName: String,
    onDisconnectClicked: () -> Unit,
) {
    WooPosCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(WooPosSpacing.Medium.value),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WooPosSettingsDetailsMenuItemInfo(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.woopos_settings_hardware_receipt_printers_device_name_title),
                subtitle = printerName,
            )

            Spacer(modifier = Modifier.size(WooPosSpacing.Medium.value))

            WooPosOutlinedButtonSmall(
                text = stringResource(R.string.woopos_settings_hardware_receipt_printers_disconnect_button),
                onClick = onDisconnectClicked
            )
        }
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsHardwareReceiptPrinterScreenDisconnectedPreview() {
    WooPosTheme {
        WooPosSettingsHardwareReceiptPrinterContent(
            uiState = WooPosSettingsHardwareReceiptPrinterUiState.Disconnected,
            onConnectClicked = { },
            onDisconnectClicked = { },
        )
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsHardwareReceiptPrinterScreenConnectedPreview() {
    WooPosTheme {
        WooPosSettingsHardwareReceiptPrinterContent(
            uiState = WooPosSettingsHardwareReceiptPrinterUiState.Connected(printerName = "Star mC-Print3"),
            onConnectClicked = { },
            onDisconnectClicked = { },
        )
    }
}
