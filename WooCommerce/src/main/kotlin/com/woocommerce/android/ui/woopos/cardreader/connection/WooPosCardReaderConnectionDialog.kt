package com.woocommerce.android.ui.woopos.cardreader.connection

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderOnboardingActivity
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosUpdateProgressIndicator
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import kotlinx.coroutines.delay

@Composable
fun WooPosCardReaderConnectionDialog(
    viewModel: WooPosCardReaderConnectionViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onConnectionSuccess: () -> Unit,
) {
    WooPosCardReaderDialogInternal(
        viewModel = viewModel,
        onStart = { it.startConnectionFlow() },
        onDismiss = onDismiss,
        onConnectionSuccess = onConnectionSuccess,
    )
}

@Composable
fun WooPosCardReaderUpdateDialog(
    viewModel: WooPosCardReaderConnectionViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onUpdateComplete: () -> Unit,
) {
    WooPosCardReaderDialogInternal(
        viewModel = viewModel,
        onStart = { it.startUpdateFlow() },
        onDismiss = onDismiss,
        onConnectionSuccess = onUpdateComplete,
    )
}

@Composable
private fun WooPosCardReaderDialogInternal(
    viewModel: WooPosCardReaderConnectionViewModel,
    onStart: (WooPosCardReaderConnectionViewModel) -> Unit,
    onDismiss: () -> Unit,
    onConnectionSuccess: () -> Unit,
) {
    val connectionState by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        val shouldShowRationale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                context as Activity,
                Manifest.permission.BLUETOOTH_SCAN
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            true
        }
        viewModel.onBluetoothPermissionResult(allGranted, shouldShowRationale)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            context as Activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        viewModel.onLocationPermissionResult(granted, shouldShowRationale)
    }

    val locationSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onLocationEnabled()
    }

    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onBluetoothEnabled()
    }

    val onboardingLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onOnboardingCompleted()
        } else {
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        onStart(viewModel)
    }

    LaunchedEffect(connectionState) {
        if (connectionState is WooPosCardReaderConnectionState.Connected ||
            connectionState is WooPosCardReaderConnectionState.UpdateCompleted
        ) {
            delay(1500)
            viewModel.dismissDialog()
            onConnectionSuccess()
        }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.onResume()
        }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.event.collect { event ->
                when (event) {
                    WooPosCardReaderConnectionViewModel.Event.RequestBluetoothPermission -> {
                        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            arrayOf(
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT
                            )
                        } else {
                            arrayOf(Manifest.permission.BLUETOOTH)
                        }
                        bluetoothPermissionLauncher.launch(permissions)
                    }
                    WooPosCardReaderConnectionViewModel.Event.RequestEnableBluetooth -> {
                        @Suppress("DEPRECATION")
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        bluetoothEnableLauncher.launch(enableBtIntent)
                    }
                    WooPosCardReaderConnectionViewModel.Event.RequestLocationPermission -> {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    WooPosCardReaderConnectionViewModel.Event.RequestEnableLocation -> {
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        locationSettingsLauncher.launch(intent)
                    }
                    WooPosCardReaderConnectionViewModel.Event.Dismissed -> {
                        onDismiss()
                    }
                    is WooPosCardReaderConnectionViewModel.Event.NavigateToOnboarding -> {
                        val intent = WooPosCardReaderOnboardingActivity.buildIntent(
                            context,
                            event.onboardingState
                        )
                        val options = ActivityOptionsCompat.makeCustomAnimation(
                            context,
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
                        )
                        onboardingLauncher.launch(intent, options)
                    }
                }
            }
        }
    }

    BackHandler {
        viewModel.onBackPressed()
    }

    WooPosCardReaderConnectionDialogContent(
        isVisible = true,
        state = connectionState,
        onBackPressed = { viewModel.onBackPressed() },
        onDismiss = {
            viewModel.dismissDialog()
            onDismiss()
        },
    )
}

@Suppress("CyclomaticComplexMethod")
@Composable
fun WooPosCardReaderConnectionDialogContent(
    isVisible: Boolean,
    state: WooPosCardReaderConnectionState,
    onBackPressed: () -> Unit,
    onDismiss: () -> Unit,
) {
    WooPosDialogWrapper(
        isVisible = isVisible,
        dialogBackgroundContentDescription = stringResource(
            R.string.woopos_card_reader_connection_dialog_background_content_description
        ),
        widthFraction = 0.5f,
        onDismissRequest = onBackPressed,
    ) {
        Column(
            modifier = Modifier.padding(WooPosSpacing.XLarge.value)
        ) {
            if (state.showCloseButton) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_close_24dp),
                            contentDescription = stringResource(R.string.woopos_card_reader_close_content_description),
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300)) using
                        SizeTransform(clip = false)
                },
                contentKey = { it::class },
                label = "CardReaderConnectionStateTransition",
                modifier = Modifier.fillMaxWidth()
            ) { currentState ->
                when (currentState) {
                    is WooPosCardReaderConnectionState.Scanning -> {
                        ScanningContent()
                    }
                    is WooPosCardReaderConnectionState.ReaderFound -> {
                        ReaderFoundContent(
                            readerName = currentState.reader.name,
                            onConnectClicked = currentState.reader.onConnectClicked,
                            onKeepSearchingClicked = currentState.onKeepSearchingClicked,
                        )
                    }
                    is WooPosCardReaderConnectionState.MultipleReadersFound -> {
                        MultipleReadersFoundContent(
                            readers = currentState.readers,
                        )
                    }
                    is WooPosCardReaderConnectionState.Connecting -> {
                        ConnectingContent()
                    }
                    is WooPosCardReaderConnectionState.Connected -> {
                        ConnectedContent(readerName = currentState.readerName)
                    }
                    is WooPosCardReaderConnectionState.ScanningFailed -> {
                        ErrorContent(
                            title = stringResource(R.string.woopos_card_reader_scanning_failed_title),
                            message = currentState.errorMessage,
                            onRetryClicked = currentState.onRetryClicked,
                            onCancelClicked = currentState.onCancelClicked,
                        )
                    }
                    is WooPosCardReaderConnectionState.ConnectingFailed -> {
                        ErrorContent(
                            title = stringResource(R.string.woopos_card_reader_connecting_failed_title),
                            message = currentState.errorMessage,
                            onRetryClicked = currentState.onRetryClicked,
                            onCancelClicked = currentState.onCancelClicked,
                        )
                    }
                    is WooPosCardReaderConnectionState.ConnectingFailedBatteryLow -> {
                        BatteryLowErrorContent(
                            onCancelClicked = currentState.onCancelClicked,
                        )
                    }
                    is WooPosCardReaderConnectionState.BluetoothDisabled -> {
                        BluetoothDisabledContent(
                            onEnableBluetoothClicked = currentState.onEnableBluetoothClicked,
                            onCancelClicked = currentState.onCancelClicked,
                        )
                    }
                    is WooPosCardReaderConnectionState.LocationDisabled -> {
                        LocationDisabledContent(
                            onEnableLocationClicked = currentState.onEnableLocationClicked,
                            onCancelClicked = currentState.onCancelClicked,
                        )
                    }
                    is WooPosCardReaderConnectionState.MissingLocationPermission -> {
                        MissingPermissionContent(
                            title = stringResource(R.string.woopos_card_reader_location_permission_title),
                            message = stringResource(R.string.woopos_card_reader_location_permission_message),
                            onRequestPermissionClicked = currentState.onRequestPermissionClicked,
                            onCancelClicked = currentState.onCancelClicked,
                        )
                    }
                    is WooPosCardReaderConnectionState.MissingBluetoothPermission -> {
                        MissingPermissionContent(
                            title = stringResource(R.string.woopos_card_reader_bluetooth_permission_title),
                            message = stringResource(R.string.woopos_card_reader_bluetooth_permission_message),
                            onRequestPermissionClicked = currentState.onRequestPermissionClicked,
                            onCancelClicked = currentState.onCancelClicked,
                        )
                    }
                    is WooPosCardReaderConnectionState.InvalidMerchantAddress -> {
                        ErrorContent(
                            title = stringResource(R.string.woopos_card_reader_invalid_address_title),
                            message = stringResource(R.string.woopos_card_reader_invalid_address_message),
                            onRetryClicked = null,
                            onCancelClicked = currentState.onCancelClicked,
                        )
                    }
                    is WooPosCardReaderConnectionState.InvalidPostalCode -> {
                        ErrorContent(
                            title = stringResource(R.string.woopos_card_reader_invalid_postal_code_title),
                            message = stringResource(R.string.woopos_card_reader_invalid_postal_code_message),
                            onRetryClicked = null,
                            onCancelClicked = currentState.onCancelClicked,
                        )
                    }
                    is WooPosCardReaderConnectionState.UpdateRequired -> {
                        UpdateRequiredContent(
                            progress = currentState.progress,
                            showCancelWarning = currentState.showCancelWarning,
                            onCancelClicked = currentState.onCancelClicked,
                        )
                    }
                    is WooPosCardReaderConnectionState.UpdateAvailable -> {
                        UpdateOptionalContent(
                            progress = currentState.progress,
                            showCancelWarning = currentState.showCancelWarning,
                            onCancelClicked = currentState.onCancelClicked,
                        )
                    }
                    is WooPosCardReaderConnectionState.UpdateCompleted -> {
                        UpdateCompletedContent()
                    }
                    is WooPosCardReaderConnectionState.UpdateFailed -> {
                        ErrorContent(
                            title = stringResource(R.string.woopos_card_reader_update_failed_title),
                            message = currentState.errorMessage,
                            onRetryClicked = currentState.onRetryClicked,
                            onCancelClicked = currentState.onCancelClicked,
                        )
                    }
                    is WooPosCardReaderConnectionState.UpdateFailedBatteryLow -> {
                        UpdateBatteryLowErrorContent(
                            currentBatteryLevel = currentState.currentBatteryLevel,
                            onCancelClicked = currentState.onCancelClicked,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardReaderDialogContent(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    CardReaderDialogContent(
        title = title,
        visual = {
            Image(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(width = 160.dp, height = 143.dp),
            )
        },
        content = content,
    )
}

@Composable
private fun CardReaderDialogContent(
    title: String,
    visual: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        WooPosText(
            text = title,
            style = WooPosTypography.Heading,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

        visual()

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

        content()
    }
}

@Composable
private fun ScanningContent() {
    CardReaderDialogContent(
        title = stringResource(R.string.woopos_card_reader_scanning_title),
        icon = WooPosIcons.CardReaderScanning,
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_card_reader_scanning_instruction),
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ReaderFoundContent(
    readerName: String,
    onConnectClicked: () -> Unit,
    onKeepSearchingClicked: () -> Unit,
) {
    CardReaderDialogContent(
        title = stringResource(R.string.woopos_card_reader_found_title, readerName),
        icon = WooPosIcons.CardReaderFound,
    ) {
        WooPosButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.woopos_card_reader_connect_button),
            onClick = onConnectClicked,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        WooPosOutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.woopos_card_reader_keep_searching_button),
            onClick = onKeepSearchingClicked,
        )
    }
}

@Composable
private fun MultipleReadersFoundContent(readers: List<WooPosCardReaderConnectionState.FoundReader>) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_card_reader_multiple_found_title),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        WooPosText(
            text = stringResource(R.string.woopos_card_reader_multiple_found_description),
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

        readers.forEachIndexed { index, reader ->
            WooPosOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                text = reader.name,
                maxLines = 1,
                onClick = reader.onConnectClicked,
            )
            if (index < readers.lastIndex) {
                Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))
            }
        }
    }
}

@Composable
private fun ConnectingContent() {
    CardReaderDialogContent(
        title = stringResource(R.string.woopos_card_reader_connecting_title),
        icon = WooPosIcons.CardReaderConnecting,
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_card_reader_connecting_message),
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ConnectedContent(readerName: String) {
    CardReaderDialogContent(
        title = stringResource(R.string.woopos_card_reader_connected_title),
        icon = WooPosIcons.CardReaderSuccess,
    ) {
        WooPosText(
            text = readerName,
            style = WooPosTypography.BodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ErrorContent(
    title: String,
    message: String,
    onRetryClicked: (() -> Unit)?,
    onCancelClicked: () -> Unit,
) {
    CardReaderDialogContent(
        title = title,
        icon = WooPosIcons.CardReaderError,
    ) {
        WooPosText(
            text = message,
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

        if (onRetryClicked != null) {
            WooPosButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.woopos_card_reader_retry_button),
                onClick = onRetryClicked,
            )
            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))
        }

        WooPosOutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.woopos_card_reader_cancel_button),
            onClick = onCancelClicked,
        )
    }
}

@Composable
private fun BatteryLowErrorContent(
    onCancelClicked: () -> Unit,
) {
    ErrorContent(
        title = stringResource(R.string.woopos_card_reader_battery_low_title),
        message = stringResource(R.string.woopos_card_reader_battery_low_message),
        onRetryClicked = null,
        onCancelClicked = onCancelClicked,
    )
}

@Composable
private fun BluetoothDisabledContent(
    onEnableBluetoothClicked: () -> Unit,
    onCancelClicked: () -> Unit,
) {
    CardReaderDialogContent(
        title = stringResource(R.string.woopos_card_reader_bluetooth_disabled_title),
        icon = WooPosIcons.CardReaderError,
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_card_reader_bluetooth_disabled_message),
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

        WooPosButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.woopos_card_reader_enable_bluetooth_button),
            onClick = onEnableBluetoothClicked,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        WooPosOutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.woopos_card_reader_cancel_button),
            onClick = onCancelClicked,
        )
    }
}

@Composable
private fun LocationDisabledContent(
    onEnableLocationClicked: () -> Unit,
    onCancelClicked: () -> Unit,
) {
    CardReaderDialogContent(
        title = stringResource(R.string.woopos_card_reader_location_disabled_title),
        icon = WooPosIcons.CardReaderError,
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_card_reader_location_disabled_message),
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

        WooPosButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.woopos_card_reader_enable_location_button),
            onClick = onEnableLocationClicked,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        WooPosOutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.woopos_card_reader_cancel_button),
            onClick = onCancelClicked,
        )
    }
}

@Composable
private fun MissingPermissionContent(
    title: String,
    message: String,
    onRequestPermissionClicked: () -> Unit,
    onCancelClicked: () -> Unit,
) {
    CardReaderDialogContent(
        title = title,
        icon = WooPosIcons.CardReaderError,
    ) {
        WooPosText(
            text = message,
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

        WooPosButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.woopos_card_reader_grant_permission_button),
            onClick = onRequestPermissionClicked,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        WooPosOutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.woopos_card_reader_cancel_button),
            onClick = onCancelClicked,
        )
    }
}

@Composable
private fun UpdateRequiredContent(
    progress: Float,
    showCancelWarning: Boolean,
    onCancelClicked: () -> Unit,
) {
    CardReaderDialogContent(
        title = stringResource(R.string.woopos_card_reader_updating_title),
        visual = {
            WooPosUpdateProgressIndicator(
                progress = progress,
                isComplete = false,
                modifier = Modifier.size(120.dp)
            )
        },
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_card_reader_update_progress, (progress * 100).toInt()),
            style = WooPosTypography.BodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (showCancelWarning) {
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosText(
                text = stringResource(R.string.woopos_card_reader_update_required_message),
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

            WooPosOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.woopos_card_reader_cancel_anyway_button),
                onClick = onCancelClicked,
            )
        }
    }
}

@Composable
private fun UpdateOptionalContent(
    progress: Float,
    showCancelWarning: Boolean,
    onCancelClicked: () -> Unit,
) {
    val message = if (showCancelWarning) {
        stringResource(R.string.woopos_card_reader_update_optional_cancel_warning)
    } else {
        stringResource(R.string.woopos_card_reader_update_optional_message)
    }

    CardReaderDialogContent(
        title = stringResource(R.string.woopos_card_reader_updating_optional_title),
        visual = {
            WooPosUpdateProgressIndicator(
                progress = progress,
                isComplete = false,
                modifier = Modifier.size(120.dp)
            )
        },
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_card_reader_update_progress, (progress * 100).toInt()),
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        WooPosText(
            text = message,
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        if (showCancelWarning) {
            Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

            WooPosOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.woopos_card_reader_cancel_anyway_button),
                onClick = onCancelClicked,
            )
        }
    }
}

@Composable
private fun UpdateCompletedContent() {
    CardReaderDialogContent(
        title = stringResource(R.string.woopos_card_reader_update_completed_title),
        visual = {
            WooPosUpdateProgressIndicator(
                progress = 1f,
                isComplete = true,
                modifier = Modifier.size(140.dp)
            )
        },
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_card_reader_update_progress, 100),
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun UpdateBatteryLowErrorContent(
    currentBatteryLevel: Float?,
    onCancelClicked: () -> Unit,
) {
    val batteryMessage = if (currentBatteryLevel != null) {
        stringResource(
            R.string.woopos_card_reader_update_battery_low_message_with_level,
            (currentBatteryLevel * 100).toInt()
        )
    } else {
        stringResource(R.string.woopos_card_reader_update_battery_low_message)
    }

    ErrorContent(
        title = stringResource(R.string.woopos_card_reader_update_failed_title),
        message = batteryMessage,
        onRetryClicked = null,
        onCancelClicked = onCancelClicked,
    )
}

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogScanningPreview() {
    WooPosTheme {
        WooPosCardReaderConnectionDialogContent(
            isVisible = true,
            state = WooPosCardReaderConnectionState.Scanning,
            onBackPressed = {},
            onDismiss = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogConnectedPreview() {
    WooPosTheme {
        WooPosCardReaderConnectionDialogContent(
            isVisible = true,
            state = WooPosCardReaderConnectionState.Connected(readerName = "STRM261380012691"),
            onBackPressed = {},
            onDismiss = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogReaderFoundPreview() {
    WooPosTheme {
        ReaderFoundContent(
            readerName = "STRM261380012691",
            onConnectClicked = {},
            onKeepSearchingClicked = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogMultipleReadersPreview() {
    WooPosTheme {
        MultipleReadersFoundContent(
            readers = listOf(
                WooPosCardReaderConnectionState.FoundReader(
                    id = "STRM261380012691",
                    name = "STRM261380012691",
                    onConnectClicked = {}
                ),
                WooPosCardReaderConnectionState.FoundReader(
                    id = "STRM261380012692",
                    name = "STRM261380012692",
                    onConnectClicked = {}
                )
            ),
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogConnectingPreview() {
    WooPosTheme {
        WooPosCardReaderConnectionDialogContent(
            isVisible = true,
            state = WooPosCardReaderConnectionState.Connecting,
            onBackPressed = {},
            onDismiss = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogConnectingFailedPreview() {
    WooPosTheme {
        WooPosCardReaderConnectionDialogContent(
            isVisible = true,
            state = WooPosCardReaderConnectionState.ConnectingFailed(
                errorMessage = "The reader has a critically low battery. Please charge the reader before use.",
                onRetryClicked = {},
                onCancelClicked = {},
            ),
            onBackPressed = {},
            onDismiss = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogBatteryLowPreview() {
    WooPosTheme {
        WooPosCardReaderConnectionDialogContent(
            isVisible = true,
            state = WooPosCardReaderConnectionState.ConnectingFailedBatteryLow(
                onCancelClicked = {},
            ),
            onBackPressed = {},
            onDismiss = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogBluetoothDisabledPreview() {
    WooPosTheme {
        WooPosCardReaderConnectionDialogContent(
            isVisible = true,
            state = WooPosCardReaderConnectionState.BluetoothDisabled(
                onEnableBluetoothClicked = {},
                onCancelClicked = {},
            ),
            onBackPressed = {},
            onDismiss = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogLocationDisabledPreview() {
    WooPosTheme {
        WooPosCardReaderConnectionDialogContent(
            isVisible = true,
            state = WooPosCardReaderConnectionState.LocationDisabled(
                onEnableLocationClicked = {},
                onCancelClicked = {},
            ),
            onBackPressed = {},
            onDismiss = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogMissingBluetoothPermissionPreview() {
    WooPosTheme {
        WooPosCardReaderConnectionDialogContent(
            isVisible = true,
            state = WooPosCardReaderConnectionState.MissingBluetoothPermission(
                onRequestPermissionClicked = {},
                onCancelClicked = {},
            ),
            onBackPressed = {},
            onDismiss = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogMissingLocationPermissionPreview() {
    WooPosTheme {
        WooPosCardReaderConnectionDialogContent(
            isVisible = true,
            state = WooPosCardReaderConnectionState.MissingLocationPermission(
                onRequestPermissionClicked = {},
                onCancelClicked = {},
            ),
            onBackPressed = {},
            onDismiss = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogUpdateRequiredPreview() {
    WooPosTheme {
        WooPosCardReaderConnectionDialogContent(
            isVisible = true,
            state = WooPosCardReaderConnectionState.UpdateRequired(
                progress = 0.45f,
                showCancelWarning = false,
                onCancelClicked = {},
                onBackClicked = {},
            ),
            onBackPressed = {},
            onDismiss = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogUpdateRequiredWithWarningPreview() {
    WooPosTheme {
        WooPosCardReaderConnectionDialogContent(
            isVisible = true,
            state = WooPosCardReaderConnectionState.UpdateRequired(
                progress = 0.45f,
                showCancelWarning = true,
                onCancelClicked = {},
                onBackClicked = {},
            ),
            onBackPressed = {},
            onDismiss = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogUpdateCompletedPreview() {
    WooPosTheme {
        WooPosCardReaderConnectionDialogContent(
            isVisible = true,
            state = WooPosCardReaderConnectionState.UpdateCompleted,
            onBackPressed = {},
            onDismiss = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogUpdateFailedPreview() {
    WooPosTheme {
        WooPosCardReaderConnectionDialogContent(
            isVisible = true,
            state = WooPosCardReaderConnectionState.UpdateFailed(
                errorMessage = "Software update failed. Please try again.",
                onRetryClicked = {},
                onCancelClicked = {},
            ),
            onBackPressed = {},
            onDismiss = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogScanningFailedPreview() {
    WooPosTheme {
        WooPosCardReaderConnectionDialogContent(
            isVisible = true,
            state = WooPosCardReaderConnectionState.ScanningFailed(
                errorMessage = "Could not find any card readers. Please try again.",
                onRetryClicked = {},
                onCancelClicked = {},
            ),
            onBackPressed = {},
            onDismiss = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogInvalidAddressPreview() {
    WooPosTheme {
        WooPosCardReaderConnectionDialogContent(
            isVisible = true,
            state = WooPosCardReaderConnectionState.InvalidMerchantAddress(
                onCancelClicked = {},
            ),
            onBackPressed = {},
            onDismiss = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogInvalidPostalCodePreview() {
    WooPosTheme {
        WooPosCardReaderConnectionDialogContent(
            isVisible = true,
            state = WooPosCardReaderConnectionState.InvalidPostalCode(
                onCancelClicked = {},
            ),
            onBackPressed = {},
            onDismiss = {},
        )
    }
}
