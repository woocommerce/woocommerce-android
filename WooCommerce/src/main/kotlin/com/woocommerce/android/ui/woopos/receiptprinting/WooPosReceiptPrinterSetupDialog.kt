package com.woocommerce.android.ui.woopos.receiptprinting

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.woocommerce.android.R
import com.woocommerce.android.receiptprinter.ReceiptPrinterDevice
import com.woocommerce.android.receiptprinter.ReceiptPrinterError
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosConnectionDialogContent
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosFoundDeviceRow
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.rememberWooPosBluetoothPermissionRequest
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.BluetoothOff
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.Connected
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.Connecting
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.ConnectionFailed
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.DiscoveryFailed
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.FoundMultiple
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.FoundOne
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.Idle
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.MissingBluetoothPermission
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.NoPrintersFound
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.Searching
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.ReceiptPrinterSetupSource

@Composable
fun WooPosReceiptPrinterSetupDialog(
    source: ReceiptPrinterSetupSource,
    viewModel: WooPosReceiptPrinterSetupViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onConnectionSuccess: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalActivity.current
    val currentOnConnectionSuccess by rememberUpdatedState(onConnectionSuccess)

    val requestBluetoothPermission =
        rememberWooPosBluetoothPermissionRequest(viewModel::onBluetoothPermissionResult)

    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onBluetoothEnabled()
    }

    LaunchedEffect(Unit) {
        viewModel.startSetup(source)
    }

    // The host can replace this dialog without dismissing it, so the setup is cancelled whenever the dialog leaves
    // the screen, except when the Activity is only being recreated.
    DisposableEffect(viewModel) {
        onDispose {
            if (activity?.isChangingConfigurations != true) viewModel.dismissDialog()
        }
    }

    LaunchedEffect(state) {
        if (state is Connected) {
            viewModel.onConnected(source)
            currentOnConnectionSuccess()
        }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.onResume()
        }
    }

    WooPosReceiptPrinterSetupDialogContent(
        state = state,
        onDismiss = onDismiss,
        onGrantPermissionClicked = requestBluetoothPermission,
        onOpenAppSettingsClicked = viewModel::onOpenAppSettingsClicked,
        onEnableBluetoothClicked = {
            bluetoothEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        },
        onConnectClicked = viewModel::onConnectClicked,
        onKeepSearchingClicked = viewModel::onKeepSearchingClicked,
        onSearchAgainClicked = viewModel::onSearchAgainClicked,
        onOpenBluetoothSettingsClicked = viewModel::onOpenBluetoothSettingsClicked,
    )
}

@Composable
private fun WooPosReceiptPrinterSetupDialogContent(
    state: WooPosReceiptPrinterConnectionState,
    onDismiss: () -> Unit,
    onGrantPermissionClicked: () -> Unit,
    onOpenAppSettingsClicked: () -> Unit,
    onEnableBluetoothClicked: () -> Unit,
    onConnectClicked: (ReceiptPrinterDevice) -> Unit,
    onKeepSearchingClicked: () -> Unit,
    onSearchAgainClicked: () -> Unit,
    onOpenBluetoothSettingsClicked: () -> Unit,
) {
    WooPosDialogWrapper(
        isVisible = true,
        dialogBackgroundContentDescription = stringResource(
            R.string.woopos_receipt_printer_setup_dialog_background_content_description
        ),
        widthFraction = 0.55f,
        onCloseClick = if (state.showsCloseButton()) onDismiss else null,
        onDismissRequest = onDismiss,
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(300)) using
                    SizeTransform(clip = false)
            },
            contentKey = { it::class },
            label = "ReceiptPrinterSetupStateTransition",
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) { currentState ->
            when (currentState) {
                Idle,
                Searching -> IllustratedContent(
                    title = stringResource(R.string.woopos_receipt_printer_setup_searching_title),
                    message = stringResource(R.string.woopos_receipt_printer_setup_searching_message),
                )

                is FoundOne -> MessageContent(
                    title = stringResource(
                        R.string.woopos_receipt_printer_setup_found_title,
                        "${currentState.device.name} · ${currentState.device.idSuffix()}",
                    ),
                    message = stringResource(R.string.woopos_receipt_printer_setup_found_message),
                    icon = WooPosIcons.ReceiptPrinter,
                    primaryButton = SetupButton(
                        text = stringResource(R.string.woopos_receipt_printer_setup_connect_to_printer_button),
                        onClick = { onConnectClicked(currentState.device) },
                    ),
                    secondaryButton = SetupButton(
                        text = stringResource(R.string.woopos_receipt_printer_setup_keep_searching_button),
                        onClick = onKeepSearchingClicked,
                    ),
                )

                is FoundMultiple -> MultiplePrintersFoundContent(
                    devices = currentState.devices,
                    onConnectClicked = onConnectClicked,
                )

                is Connecting,
                is Connected -> IllustratedContent(
                    title = stringResource(R.string.woopos_receipt_printer_setup_connecting_title),
                    message = stringResource(R.string.woopos_receipt_printer_setup_connecting_message),
                )

                NoPrintersFound -> MessageContent(
                    title = stringResource(R.string.woopos_receipt_printer_setup_no_printers_title),
                    message = stringResource(R.string.woopos_receipt_printer_setup_no_printers_message),
                    icon = WooPosIcons.NotFound,
                    primaryButton = SetupButton(
                        text = stringResource(R.string.woopos_receipt_printer_setup_search_again_button),
                        onClick = onSearchAgainClicked,
                    ),
                    link = SetupButton(
                        text = stringResource(R.string.woopos_receipt_printer_setup_open_bluetooth_settings_button),
                        onClick = onOpenBluetoothSettingsClicked,
                    ),
                    onCancelClicked = onDismiss,
                )

                DiscoveryFailed -> MessageContent(
                    title = stringResource(R.string.woopos_receipt_printer_setup_discovery_failed_title),
                    message = stringResource(R.string.woopos_receipt_printer_setup_discovery_failed_message),
                    icon = WooPosIcons.ErrorX,
                    primaryButton = SetupButton(
                        text = stringResource(R.string.woopos_receipt_printer_setup_search_again_button),
                        onClick = onSearchAgainClicked,
                    ),
                    onCancelClicked = onDismiss,
                )

                is ConnectionFailed -> MessageContent(
                    title = stringResource(R.string.woopos_receipt_printer_setup_connection_failed_title),
                    message = stringResource(R.string.woopos_receipt_printer_setup_connection_failed_message),
                    icon = WooPosIcons.ErrorX,
                    primaryButton = SetupButton(
                        text = stringResource(R.string.woopos_receipt_printer_setup_search_again_button),
                        onClick = onSearchAgainClicked,
                    ),
                    onCancelClicked = onDismiss,
                )

                is MissingBluetoothPermission -> MissingBluetoothPermissionContent(
                    permanentlyDenied = currentState.permanentlyDenied,
                    onGrantPermissionClicked = onGrantPermissionClicked,
                    onOpenAppSettingsClicked = onOpenAppSettingsClicked,
                    onCancelClicked = onDismiss,
                )

                BluetoothOff -> MessageContent(
                    title = stringResource(R.string.woopos_receipt_printer_setup_bluetooth_off_title),
                    message = stringResource(R.string.woopos_receipt_printer_setup_bluetooth_off_message),
                    icon = WooPosIcons.BluetoothSettings,
                    primaryButton = SetupButton(
                        text = stringResource(R.string.woopos_receipt_printer_setup_turn_on_bluetooth_button),
                        onClick = onEnableBluetoothClicked,
                    ),
                    onCancelClicked = onDismiss,
                )
            }
        }
    }
}

private fun WooPosReceiptPrinterConnectionState.showsCloseButton(): Boolean = when (this) {
    Idle, Searching, is FoundOne, is FoundMultiple, is Connecting, is Connected -> true
    is MissingBluetoothPermission, BluetoothOff, NoPrintersFound, DiscoveryFailed, is ConnectionFailed -> false
}

private data class SetupButton(
    val text: String,
    val onClick: () -> Unit,
)

@Composable
private fun IllustratedContent(title: String, message: String) {
    WooPosConnectionDialogContent(
        title = title,
        visual = {
            Image(
                imageVector = WooPosIcons.ReceiptPrinter,
                contentDescription = null,
                modifier = Modifier.size(WooPosComponentSize.XLarge.value),
            )
        },
    ) {
        WooPosText(
            text = message,
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))
    }
}

@Composable
private fun MessageContent(
    title: String,
    message: String,
    icon: ImageVector,
    primaryButton: SetupButton,
    secondaryButton: SetupButton? = null,
    link: SetupButton? = null,
    onCancelClicked: (() -> Unit)? = null,
) {
    WooPosConnectionDialogContent(
        title = title,
        visual = {
            Image(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(WooPosComponentSize.XLarge.value),
            )
        },
    ) {
        WooPosText(
            text = message,
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        if (link != null) {
            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

            WooPosText(
                text = link.text,
                style = WooPosTypography.BodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = link.onClick)
                    .padding(horizontal = WooPosSpacing.Large.value, vertical = WooPosSpacing.Small.value),
            )
        }

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

        WooPosButton(
            modifier = Modifier.fillMaxWidth(),
            text = primaryButton.text,
            onClick = primaryButton.onClick,
        )

        if (secondaryButton != null) {
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                text = secondaryButton.text,
                onClick = secondaryButton.onClick,
            )
        }

        if (onCancelClicked != null) {
            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

            WooPosOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.woopos_receipt_printer_setup_cancel_button),
                onClick = onCancelClicked,
            )
        }
    }
}

@Composable
private fun MissingBluetoothPermissionContent(
    permanentlyDenied: Boolean,
    onGrantPermissionClicked: () -> Unit,
    onOpenAppSettingsClicked: () -> Unit,
    onCancelClicked: () -> Unit,
) {
    val title = stringResource(R.string.woopos_receipt_printer_setup_bluetooth_permission_title)
    if (permanentlyDenied) {
        MessageContent(
            title = title,
            message = stringResource(R.string.woopos_receipt_printer_setup_bluetooth_permission_denied_message),
            icon = WooPosIcons.BluetoothSettings,
            primaryButton = SetupButton(
                text = stringResource(R.string.woopos_receipt_printer_setup_open_settings_button),
                onClick = onOpenAppSettingsClicked,
            ),
            onCancelClicked = onCancelClicked,
        )
    } else {
        MessageContent(
            title = title,
            message = stringResource(R.string.woopos_receipt_printer_setup_bluetooth_permission_message),
            icon = WooPosIcons.BluetoothSettings,
            primaryButton = SetupButton(
                text = stringResource(R.string.woopos_receipt_printer_setup_grant_permission_button),
                onClick = onGrantPermissionClicked,
            ),
            onCancelClicked = onCancelClicked,
        )
    }
}

@Composable
private fun MultiplePrintersFoundContent(
    devices: List<ReceiptPrinterDevice>,
    onConnectClicked: (ReceiptPrinterDevice) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_receipt_printer_setup_multiple_found_title),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        WooPosText(
            text = stringResource(R.string.woopos_receipt_printer_setup_multiple_found_description),
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

        devices.forEach { device ->
            WooPosFoundDeviceRow(
                name = device.name,
                suffix = device.idSuffix(),
                icon = painterResource(R.drawable.ic_bluetooth_24dp),
                iconContentDescription = stringResource(
                    R.string.woopos_receipt_printer_setup_printer_icon_content_description
                ),
                onClick = { onConnectClicked(device) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))
        }
    }
}

private fun ReceiptPrinterDevice.idSuffix(): String =
    id.filter { it.isLetterOrDigit() }.takeLast(ID_SUFFIX_LENGTH).uppercase()

@Composable
private fun SetupDialogPreview(state: WooPosReceiptPrinterConnectionState) {
    WooPosTheme {
        WooPosReceiptPrinterSetupDialogContent(
            state = state,
            onDismiss = {},
            onGrantPermissionClicked = {},
            onOpenAppSettingsClicked = {},
            onEnableBluetoothClicked = {},
            onConnectClicked = {},
            onKeepSearchingClicked = {},
            onSearchAgainClicked = {},
            onOpenBluetoothSettingsClicked = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosReceiptPrinterSetupDialogSearchingPreview() {
    SetupDialogPreview(Searching)
}

@WooPosPreview
@Composable
fun WooPosReceiptPrinterSetupDialogFoundOnePreview() {
    SetupDialogPreview(FoundOne(ReceiptPrinterDevice(id = "00:11:62:4C:73:F0", name = "Star mC-Print3")))
}

@WooPosPreview
@Composable
fun WooPosReceiptPrinterSetupDialogFoundMultiplePreview() {
    SetupDialogPreview(
        FoundMultiple(
            listOf(
                ReceiptPrinterDevice(id = "00:11:62:4C:73:F0", name = "Star mC-Print3"),
                ReceiptPrinterDevice(id = "00:11:62:5A:19:2B", name = "Star mC-Print3"),
                ReceiptPrinterDevice(id = "00:11:62:61:08:C7", name = "TSP100IV-D0029"),
            )
        )
    )
}

@WooPosPreview
@Composable
fun WooPosReceiptPrinterSetupDialogConnectingPreview() {
    SetupDialogPreview(Connecting(ReceiptPrinterDevice(id = "1", name = "Star mC-Print3")))
}

@WooPosPreview
@Composable
fun WooPosReceiptPrinterSetupDialogNoPrintersFoundPreview() {
    SetupDialogPreview(NoPrintersFound)
}

@WooPosPreview
@Composable
fun WooPosReceiptPrinterSetupDialogDiscoveryFailedPreview() {
    SetupDialogPreview(DiscoveryFailed)
}

@WooPosPreview
@Composable
fun WooPosReceiptPrinterSetupDialogConnectionFailedPreview() {
    SetupDialogPreview(
        ConnectionFailed(
            device = ReceiptPrinterDevice(id = "1", name = "Star mC-Print3"),
            error = ReceiptPrinterError.ConnectionLost(),
        )
    )
}

@WooPosPreview
@Composable
fun WooPosReceiptPrinterSetupDialogMissingBluetoothPermissionPreview() {
    SetupDialogPreview(MissingBluetoothPermission(permanentlyDenied = false))
}

@WooPosPreview
@Composable
fun WooPosReceiptPrinterSetupDialogBluetoothPermissionDeniedPreview() {
    SetupDialogPreview(MissingBluetoothPermission(permanentlyDenied = true))
}

@WooPosPreview
@Composable
fun WooPosReceiptPrinterSetupDialogBluetoothOffPreview() {
    SetupDialogPreview(BluetoothOff)
}

private const val ID_SUFFIX_LENGTH = 4
