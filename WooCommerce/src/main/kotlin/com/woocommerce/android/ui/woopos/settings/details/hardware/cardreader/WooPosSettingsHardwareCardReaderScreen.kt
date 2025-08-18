package com.woocommerce.android.ui.woopos.settings.details.hardware.cardreader

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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonSmall
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.settings.details.WooPosSettingsDetailsMenuItem
import com.woocommerce.android.util.ChromeCustomTabUtils
import kotlinx.coroutines.flow.collectLatest

@Composable
fun WooPosSettingsHardwareCardReaderScreen(
    viewModel: WooPosSettingsHardwareCardReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.openUrl.collectLatest { url ->
            ChromeCustomTabUtils.launchUrl(context, url, enableSlideAnimation = true)
        }
    }

    WooPosSettingsHardwareCardReaderContent(
        uiState = uiState,
        onConnectClicked = viewModel::onConnectClicked,
        onDisconnectClicked = viewModel::onDisconnectClicked,
        onDocumentationClicked = viewModel::onDocumentationClicked
    )
}

@Composable
private fun WooPosSettingsHardwareCardReaderContent(
    uiState: WooPosSettingsHardwareCardReaderUiState,
    onConnectClicked: () -> Unit,
    onDisconnectClicked: () -> Unit,
    onDocumentationClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(WooPosSpacing.Medium.value),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
    ) {
        when (uiState) {
            is WooPosSettingsHardwareCardReaderUiState.Connecting -> {
                NotConnectedContent(
                    onConnectClicked = onConnectClicked,
                    onDocumentationClicked = onDocumentationClicked,
                    isConnecting = true
                )
            }
            is WooPosSettingsHardwareCardReaderUiState.Connected -> {
                ConnectedContent(
                    readerName = uiState.readerName,
                    batteryLevel = uiState.batteryLevel,
                    firmwareVersion = uiState.firmwareVersion ?: stringResource(
                        R.string.woopos_settings_card_reader_unknown_firmware
                    ),
                    isSoftwareUpdateAvailable = uiState.isSoftwareUpdateAvailable,
                    onDisconnectClicked = onDisconnectClicked
                )
            }
            is WooPosSettingsHardwareCardReaderUiState.Disconnected -> {
                NotConnectedContent(
                    onConnectClicked = onConnectClicked,
                    onDocumentationClicked = onDocumentationClicked,
                    isConnecting = false
                )
            }
        }
    }
}


@Composable
private fun ConnectedContent(
    readerName: String,
    batteryLevel: Float?,
    firmwareVersion: String,
    isSoftwareUpdateAvailable: Boolean,
    onDisconnectClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Medium.value),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value)
        ) {
            WooPosText(
                text = stringResource(R.string.card_reader_detail_connected_header),
                style = WooPosTypography.BodyXLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            WooPosText(
                text = readerName,
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }

    if (batteryLevel != null) {
        WooPosSettingsDetailsMenuItem(
            icon = getBatteryIcon(batteryLevel),
            title = stringResource(R.string.woopos_settings_card_reader_battery_title),
            subtitle = stringResource(
                R.string.card_reader_detail_connected_battery_percentage,
                (batteryLevel * 100).toInt()
            )
        )
    }

    FirmwareMenuItem(
        firmwareVersion = firmwareVersion,
        isSoftwareUpdateAvailable = isSoftwareUpdateAvailable,
        onUpdateClick = { }
    )

    Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

    WooPosOutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.card_reader_detail_connected_disconnect_reader),
        onClick = onDisconnectClicked
    )
}

@Composable
private fun NotConnectedContent(
    onConnectClicked: () -> Unit,
    onDocumentationClicked: () -> Unit,
    isConnecting: Boolean = false
) {
    Column {
        WooPosSettingsDetailsMenuItem(
            icon = Icons.Default.Description,
            title = stringResource(R.string.woopos_settings_card_reader_documentation_title),
            subtitle = stringResource(R.string.woopos_settings_card_reader_documentation_subtitle),
            onClick = onDocumentationClicked
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WooPosSpacing.Medium.value),
                verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
            ) {
                WooPosText(
                    text = stringResource(R.string.card_reader_detail_not_connected_header),
                    style = WooPosTypography.BodyXLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value)
                ) {
                    ConnectionHint(stringResource(R.string.card_reader_detail_not_connected_first_hint_label))
                    ConnectionHint(stringResource(R.string.card_reader_detail_not_connected_second_hint_label))
                    ConnectionHint(stringResource(R.string.card_reader_detail_not_connected_third_hint_label))
                }
            }
        }

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        WooPosButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.card_reader_details_not_connected_connect_button_label),
            state = if (isConnecting) WooPosButtonState.LOADING else WooPosButtonState.ENABLED,
            onClick = onConnectClicked
        )
    }
}

@Composable
private fun FirmwareMenuItem(
    firmwareVersion: String,
    isSoftwareUpdateAvailable: Boolean,
    onUpdateClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WooPosSpacing.Medium.value),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = Icons.Default.SystemUpdate,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(28.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = WooPosSpacing.Medium.value)
        ) {
            WooPosText(
                text = stringResource(R.string.woopos_settings_card_reader_firmware_title),
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.XSmall.value)
            ) {
                WooPosText(
                    text = stringResource(
                        R.string.card_reader_detail_connected_firmware_version,
                        firmwareVersion
                    ),
                    style = WooPosTypography.BodySmall,
                    color = if (isSoftwareUpdateAvailable) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.padding(top = WooPosSpacing.XSmall.value)
                )
                if (isSoftwareUpdateAvailable) {
                    WooPosText(
                        text = stringResource(R.string.woopos_settings_card_reader_update_available),
                        style = WooPosTypography.BodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = WooPosSpacing.XSmall.value)
                    )
                }
            }
        }

        if (isSoftwareUpdateAvailable) {
            WooPosButtonSmall(
                text = stringResource(R.string.woopos_settings_card_reader_update_button),
                onClick = onUpdateClick
            )
        }
    }
}

@Composable
private fun ConnectionHint(text: String) {
    WooPosText(
        text = "• $text",
        style = WooPosTypography.BodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun getBatteryIcon(batteryLevel: Float): ImageVector {
    return when {
        batteryLevel >= 1.0f -> Icons.Default.BatteryFull
        batteryLevel >= 0.86f -> Icons.Default.Battery6Bar
        batteryLevel >= 0.71f -> Icons.Default.Battery5Bar
        batteryLevel >= 0.57f -> Icons.Default.Battery4Bar
        batteryLevel >= 0.43f -> Icons.Default.Battery3Bar
        batteryLevel >= 0.29f -> Icons.Default.Battery2Bar
        batteryLevel >= 0.14f -> Icons.Default.Battery1Bar
        else -> Icons.Default.Battery0Bar
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsHardwareCardReaderScreenNotConnectedPreview() {
    WooPosTheme {
        WooPosSettingsHardwareCardReaderContent(
            uiState = WooPosSettingsHardwareCardReaderUiState.Disconnected,
            onConnectClicked = { },
            onDisconnectClicked = { },
            onDocumentationClicked = { }
        )
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsHardwareCardReaderScreenConnectedPreview() {
    WooPosTheme {
        WooPosSettingsHardwareCardReaderContent(
            uiState = WooPosSettingsHardwareCardReaderUiState.Connected(
                readerName = "Stripe Reader M2",
                batteryLevel = 0.75f,
                firmwareVersion = "1.2.3",
                isSoftwareUpdateAvailable = true
            ),
            onConnectClicked = { },
            onDisconnectClicked = { },
            onDocumentationClicked = { }
        )
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsHardwareCardReaderScreenConnectingPreview() {
    WooPosTheme {
        WooPosSettingsHardwareCardReaderContent(
            uiState = WooPosSettingsHardwareCardReaderUiState.Connecting,
            onConnectClicked = { },
            onDisconnectClicked = { },
            onDocumentationClicked = { }
        )
    }
}
