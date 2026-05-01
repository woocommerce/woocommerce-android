package com.woocommerce.android.ui.woopos.settings.details.hardware.cardreader

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonSmall
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButtonSmall
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.settings.details.WooPosSettingsDetailsMenuItem
import com.woocommerce.android.ui.woopos.settings.details.WooPosSettingsDetailsMenuItemInfo
import com.woocommerce.android.util.ChromeCustomTabUtils
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

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
        onDocumentationClicked = viewModel::onDocumentationClicked,
        onUpdateClick = viewModel::onUpdateClick
    )
}

@Composable
private fun WooPosSettingsHardwareCardReaderContent(
    uiState: WooPosSettingsHardwareCardReaderUiState,
    onConnectClicked: () -> Unit,
    onDisconnectClicked: () -> Unit,
    onDocumentationClicked: () -> Unit,
    onUpdateClick: () -> Unit,
) {
    AnimatedContent(
        targetState = uiState,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "card_reader_state_animation"
    ) { state ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            when (state) {
                is WooPosSettingsHardwareCardReaderUiState.Connected -> {
                    ConnectedContent(
                        readerName = state.readerName,
                        batteryLevel = state.batteryLevel,
                        firmwareVersion = state.firmwareVersion ?: stringResource(
                            R.string.woopos_settings_card_reader_unknown_firmware
                        ),
                        isSoftwareUpdateAvailable = state.isSoftwareUpdateAvailable,
                        onDisconnectClicked = onDisconnectClicked,
                        onUpdateClick = onUpdateClick,
                        onDocumentationClicked = onDocumentationClicked,
                    )
                }

                is WooPosSettingsHardwareCardReaderUiState.Disconnected -> {
                    NotConnectedContent(
                        onConnectClicked = onConnectClicked,
                        onDocumentationClicked = onDocumentationClicked,
                    )
                }
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
    onDisconnectClicked: () -> Unit,
    onUpdateClick: () -> Unit,
    onDocumentationClicked: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = WooPosSpacing.Medium.value),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
    ) {
        if (isSoftwareUpdateAvailable) {
            UpdateFirmwareBanner()
        }

        WooPosCard(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .padding(WooPosSpacing.Medium.value)
            ) {
                WooPosSettingsDetailsMenuItemInfo(
                    title = stringResource(R.string.woopos_settings_card_reader_device_name_title),
                    subtitle = readerName,
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = WooPosSpacing.Medium.value))

                if (batteryLevel != null) {
                    WooPosSettingsDetailsMenuItemInfo(
                        title = stringResource(R.string.woopos_settings_card_reader_battery_title),
                        subtitle = "${(batteryLevel * 100).roundToInt()}%",
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = WooPosSpacing.Medium.value))
                }

                Row {
                    WooPosSettingsDetailsMenuItemInfo(
                        title = stringResource(R.string.woopos_settings_card_reader_firmware_title),
                        subtitle = stringResource(
                            R.string.card_reader_detail_connected_firmware_version,
                            firmwareVersion
                        ),
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    if (isSoftwareUpdateAvailable) {
                        Spacer(modifier = Modifier.size(WooPosSpacing.Medium.value))
                        WooPosButtonSmall(
                            text = stringResource(R.string.woopos_settings_card_reader_update_button),
                            onClick = onUpdateClick
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = WooPosSpacing.Medium.value))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ReaderConnectedIndicator()

                    Spacer(modifier = Modifier.weight(1f))

                    WooPosOutlinedButtonSmall(
                        text = stringResource(R.string.woopos_settings_card_reader_disconnect_button),
                        onClick = onDisconnectClicked
                    )
                }
            }
        }

        WooPosSettingsDetailsMenuItem(
            title = stringResource(R.string.woopos_settings_card_reader_documentation_title),
            subtitle = stringResource(R.string.woopos_settings_card_reader_documentation_subtitle),
            onClick = onDocumentationClicked
        )
    }
}

@Composable
private fun NotConnectedContent(
    onConnectClicked: () -> Unit,
    onDocumentationClicked: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
    ) {
        WooPosSettingsDetailsMenuItem(
            modifier = Modifier.padding(horizontal = WooPosSpacing.Medium.value),
            title = stringResource(R.string.card_reader_detail_not_connected_header),
            subtitle = stringResource(R.string.card_reader_detail_not_connected_first_hint_label),
            onClick = onConnectClicked
        )

        WooPosSettingsDetailsMenuItem(
            modifier = Modifier.padding(horizontal = WooPosSpacing.Medium.value),
            title = stringResource(R.string.woopos_settings_card_reader_documentation_title),
            subtitle = stringResource(R.string.woopos_settings_card_reader_documentation_subtitle),
            onClick = onDocumentationClicked
        )
    }
}

@Composable
private fun ReaderConnectedIndicator() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color = WooPosTheme.colors.success, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(WooPosSpacing.XSmall.value))
        WooPosText(
            modifier = Modifier.padding(horizontal = WooPosSpacing.Small.value),
            text = stringResource(R.string.woopos_reader_connected),
            style = WooPosTypography.BodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun UpdateFirmwareBanner() {
    WooPosCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Medium.value),
            horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_tintable_info_outline_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(WooPosIconSize.Medium.value)
            )

            Column(modifier = Modifier.weight(1f)) {
                WooPosText(
                    text = stringResource(R.string.woopos_settings_card_reader_update_firmware_title),
                    style = WooPosTypography.BodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))

                WooPosText(
                    text = stringResource(R.string.woopos_settings_card_reader_update_firmware_message),
                    style = WooPosTypography.BodySmall,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                )
            }
        }
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
            onDocumentationClicked = { },
            onUpdateClick = { }
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
            onDocumentationClicked = { },
            onUpdateClick = { }
        )
    }
}
