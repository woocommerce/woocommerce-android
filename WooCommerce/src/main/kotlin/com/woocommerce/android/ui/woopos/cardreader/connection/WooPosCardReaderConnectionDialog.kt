package com.woocommerce.android.ui.woopos.cardreader.connection

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderOnboardingActivity
import com.woocommerce.android.ui.woopos.cardreader.remote.WooPosDiscoveryTransport
import com.woocommerce.android.ui.woopos.cardreader.remote.WooPosRemoteReaderExplainerContent
import com.woocommerce.android.ui.woopos.cardreader.remote.WooPosRemoteReaderHintStrip
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularLoadingIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosUpdateProgressIndicator
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.currentWooPosBreakpoint
import com.woocommerce.android.util.ChromeCustomTabUtils
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
        onHintClick = viewModel::onRemoteTapToPayHintClicked,
    )
}

@Suppress("CyclomaticComplexMethod")
@Composable
fun WooPosCardReaderConnectionDialogContent(
    isVisible: Boolean,
    state: WooPosCardReaderConnectionState,
    onBackPressed: () -> Unit,
    onDismiss: () -> Unit,
    onHintClick: () -> Unit = {},
) {
    WooPosDialogWrapper(
        isVisible = isVisible,
        dialogBackgroundContentDescription = stringResource(
            R.string.woopos_card_reader_connection_dialog_background_content_description
        ),
        widthFraction = 0.55f,
        onCloseClick = if (state.showCloseButton) onDismiss else null,
        onDismissRequest = onBackPressed,
    ) {
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
                is WooPosCardReaderConnectionState.RemoteTapToPayExplainer -> {
                    WooPosRemoteReaderExplainerContent(onDismiss = currentState.onDismissClicked)
                }
                is WooPosCardReaderConnectionState.Scanning -> {
                    ScanningContent(
                        onHintClick = onHintClick,
                    )
                }
                is WooPosCardReaderConnectionState.ReaderFound -> {
                    ReaderFoundContent(
                        readerName = currentState.reader.name,
                        fingerprintSuffix = currentState.reader.fingerprintSuffix,
                        onConnectClicked = currentState.reader.onConnectClicked,
                        onKeepSearchingClicked = currentState.onKeepSearchingClicked,
                        bluetoothUnavailable = currentState.bluetoothUnavailable,
                    )
                }
                is WooPosCardReaderConnectionState.MultipleReadersFound -> {
                    MultipleReadersFoundContent(
                        readers = currentState.readers,
                        bluetoothUnavailable = currentState.bluetoothUnavailable,
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
                        onHintClick = onHintClick,
                    )
                }
                is WooPosCardReaderConnectionState.LocationDisabled -> {
                    LocationDisabledContent(
                        onEnableLocationClicked = currentState.onEnableLocationClicked,
                        onCancelClicked = currentState.onCancelClicked,
                        onHintClick = onHintClick,
                    )
                }
                is WooPosCardReaderConnectionState.MissingLocationPermission -> {
                    MissingPermissionContent(
                        title = stringResource(R.string.woopos_card_reader_location_permission_title),
                        message = stringResource(R.string.woopos_card_reader_location_permission_message),
                        onRequestPermissionClicked = currentState.onRequestPermissionClicked,
                        onCancelClicked = currentState.onCancelClicked,
                        onHintClick = onHintClick,
                    )
                }
                is WooPosCardReaderConnectionState.MissingBluetoothPermission -> {
                    MissingPermissionContent(
                        title = stringResource(R.string.woopos_card_reader_bluetooth_permission_title),
                        message = stringResource(R.string.woopos_card_reader_bluetooth_permission_message),
                        onRequestPermissionClicked = currentState.onRequestPermissionClicked,
                        onCancelClicked = currentState.onCancelClicked,
                        onHintClick = onHintClick,
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
                is WooPosCardReaderConnectionState.OnboardingError -> {
                    ErrorContent(
                        title = currentState.title,
                        message = currentState.message,
                        onRetryClicked = currentState.primaryButton?.onClick,
                        onCancelClicked = currentState.onDismissClicked,
                        retryButtonLabel = currentState.primaryButton?.label,
                    )
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
                modifier = Modifier.size(WooPosComponentSize.XLarge.value),
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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

        visual()

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

        content()
    }
}

@Composable
private fun ScanningContent(
    onHintClick: () -> Unit,
) {
    val showHint = currentWooPosBreakpoint() != WooPosBreakpoint.Phone
    when (showHint) {
        true -> Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            ScanningDialogBody()
            WooPosRemoteReaderHintStrip(onClick = onHintClick)
        }
        false -> ScanningDialogBody()
    }
}

@Composable
private fun ScanningDialogBody() {
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
        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))
    }
}

@Composable
private fun ReaderFoundContent(
    readerName: String,
    fingerprintSuffix: String?,
    onConnectClicked: () -> Unit,
    onKeepSearchingClicked: () -> Unit,
    bluetoothUnavailable: WooPosCardReaderConnectionState.BluetoothUnavailable?,
) {
    val title = if (fingerprintSuffix != null) {
        stringResource(R.string.woopos_card_reader_found_title, "$readerName · $fingerprintSuffix")
    } else {
        stringResource(R.string.woopos_card_reader_found_title, readerName)
    }
    CardReaderDialogContent(
        title = title,
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

        BluetoothUnavailableStrip(bluetoothUnavailable)
    }
}

@Composable
private fun BluetoothUnavailableStrip(
    bluetoothUnavailable: WooPosCardReaderConnectionState.BluetoothUnavailable?,
) {
    if (bluetoothUnavailable == null) return

    val label = when (bluetoothUnavailable.requirement) {
        WooPosCardReaderConnectionState.BluetoothRequirement.Unmet.MissingBluetoothPermission ->
            R.string.woopos_card_reader_bluetooth_unavailable_permission
        WooPosCardReaderConnectionState.BluetoothRequirement.Unmet.BluetoothOff ->
            R.string.woopos_card_reader_bluetooth_unavailable_off
        WooPosCardReaderConnectionState.BluetoothRequirement.Unmet.MissingLocationPermission ->
            R.string.woopos_card_reader_bluetooth_unavailable_location_permission
        WooPosCardReaderConnectionState.BluetoothRequirement.Unmet.LocationOff ->
            R.string.woopos_card_reader_bluetooth_unavailable_location_off
    }

    Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

    WooPosText(
        text = stringResource(label),
        style = WooPosTypography.BodyMedium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = bluetoothUnavailable.onFixClicked)
            .padding(
                horizontal = WooPosSpacing.Large.value,
                vertical = WooPosSpacing.Small.value,
            ),
    )
}

@Composable
private fun MultipleReadersFoundContent(
    readers: List<WooPosCardReaderConnectionState.FoundReader>,
    bluetoothUnavailable: WooPosCardReaderConnectionState.BluetoothUnavailable?,
) {
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

        readers.forEach { reader ->
            FoundReaderRow(
                reader = reader,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))
        }

        Row(
            modifier = Modifier
                .height(WooPosComponentSize.Small.value),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            WooPosCircularLoadingIndicator(
                modifier = Modifier.size(WooPosIconSize.Small.value)
            )
        }

        BluetoothUnavailableStrip(bluetoothUnavailable)
    }
}

@Composable
private fun FoundReaderRow(
    reader: WooPosCardReaderConnectionState.FoundReader,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = reader.onConnectClicked)
            .height(WooPosComponentSize.Small.value),
        shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
        color = WooPosTheme.colors.transparent,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.inverseSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    PaddingValues(
                        horizontal = WooPosSpacing.Medium.value,
                        vertical = WooPosSpacing.Small.value,
                    )
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val (iconRes, iconContentDescription) = when (reader.transport) {
                WooPosDiscoveryTransport.Bluetooth ->
                    R.drawable.ic_bluetooth_24dp to
                        stringResource(R.string.woopos_card_reader_bluetooth_icon_content_description)
                WooPosDiscoveryTransport.WifiLan ->
                    R.drawable.ic_smartphone_24dp to
                        stringResource(R.string.woopos_card_reader_phone_icon_content_description)
            }
            Icon(
                painter = painterResource(iconRes),
                contentDescription = iconContentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(WooPosIconSize.Small.value),
            )
            Spacer(modifier = Modifier.size(WooPosSpacing.Small.value))
            WooPosText(
                text = reader.name,
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            reader.fingerprintSuffix?.let { suffix ->
                WooPosText(
                    text = suffix,
                    style = WooPosTypography.BodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))
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
        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))
    }
}

@Composable
private fun ErrorContent(
    title: String,
    message: String,
    onRetryClicked: (() -> Unit)?,
    onCancelClicked: () -> Unit,
    retryButtonLabel: String? = null,
) {
    val context = LocalContext.current
    val linkColor = MaterialTheme.colorScheme.primary

    CardReaderDialogContent(
        title = title,
        icon = WooPosIcons.CardReaderError,
    ) {
        val annotatedMessage = buildAnnotatedStringWithLinks(
            text = message,
            linkColor = linkColor,
            onLinkClick = { url ->
                ChromeCustomTabUtils.launchUrl(context, url)
            }
        )

        WooPosText(
            text = annotatedMessage,
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

        if (onRetryClicked != null) {
            WooPosButton(
                modifier = Modifier.fillMaxWidth(),
                text = retryButtonLabel ?: stringResource(R.string.woopos_card_reader_retry_button),
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
    onHintClick: () -> Unit,
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

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        WooPosRemoteReaderHintStrip(onClick = onHintClick)
    }
}

@Composable
private fun LocationDisabledContent(
    onEnableLocationClicked: () -> Unit,
    onCancelClicked: () -> Unit,
    onHintClick: () -> Unit,
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

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        WooPosRemoteReaderHintStrip(onClick = onHintClick)
    }
}

@Composable
private fun MissingPermissionContent(
    title: String,
    message: String,
    onRequestPermissionClicked: () -> Unit,
    onCancelClicked: () -> Unit,
    onHintClick: () -> Unit,
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

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        WooPosRemoteReaderHintStrip(onClick = onHintClick)
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
                modifier = Modifier.size(WooPosComponentSize.Large.value)
            )
        },
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_card_reader_update_progress, (progress * 100).toInt()),
            style = WooPosTypography.BodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        if (showCancelWarning) {
            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

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
                modifier = Modifier.size(WooPosComponentSize.Large.value)
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
                modifier = Modifier.size(WooPosComponentSize.XLarge.value)
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

@Composable
private fun buildAnnotatedStringWithLinks(
    text: String,
    linkColor: Color,
    onLinkClick: (String) -> Unit,
) = buildAnnotatedString {
    val urlMatcher = Patterns.WEB_URL.matcher(text)
    var lastEnd = 0

    while (urlMatcher.find()) {
        val urlStart = urlMatcher.start()
        val urlEnd = urlMatcher.end()
        val url = text.substring(urlStart, urlEnd)

        append(text.substring(lastEnd, urlStart))

        val linkAnnotation = LinkAnnotation.Url(url) {
            onLinkClick(url)
        }
        withStyle(
            style = SpanStyle(
                color = linkColor,
                textDecoration = TextDecoration.Underline
            )
        ) {
            withLink(linkAnnotation) {
                append(url)
            }
        }

        lastEnd = urlEnd
    }

    if (lastEnd < text.length) {
        append(text.substring(lastEnd))
    }
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
            fingerprintSuffix = null,
            onConnectClicked = {},
            onKeepSearchingClicked = {},
            bluetoothUnavailable = null,
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogMultipleReadersPreview() {
    WooPosTheme {
        WooPosCardReaderConnectionDialogContent(
            isVisible = true,
            state = WooPosCardReaderConnectionState.MultipleReadersFound(
                readers = listOf(
                    btReader("STRM261380012691"),
                    btReader("STRM261380012692"),
                    phoneReader("Andrey's Pixel 7", "AB4F"),
                ),
                onCancelClicked = {},
                bluetoothUnavailable = null,
            ),
            onBackPressed = {},
            onDismiss = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogPhonesOnlyPreview() {
    WooPosTheme {
        WooPosCardReaderConnectionDialogContent(
            isVisible = true,
            state = WooPosCardReaderConnectionState.MultipleReadersFound(
                readers = listOf(
                    phoneReader("Andrey's Pixel 7", "AB4F"),
                    phoneReader("Sales floor phone", "3C21"),
                ),
                onCancelClicked = {},
                bluetoothUnavailable = null,
            ),
            onBackPressed = {},
            onDismiss = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogMixedReadersPreview() {
    WooPosTheme {
        WooPosCardReaderConnectionDialogContent(
            isVisible = true,
            state = WooPosCardReaderConnectionState.MultipleReadersFound(
                readers = listOf(
                    btReader("STRM261380012691"),
                    phoneReader("Andrey's Pixel 7", "AB4F"),
                ),
                onCancelClicked = {},
                bluetoothUnavailable = null,
            ),
            onBackPressed = {},
            onDismiss = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogPhonesOnlyBluetoothOffPreview() {
    WooPosTheme {
        WooPosCardReaderConnectionDialogContent(
            isVisible = true,
            state = WooPosCardReaderConnectionState.MultipleReadersFound(
                readers = listOf(
                    phoneReader("Andrey's Pixel 7", "AB4F"),
                    phoneReader("Sales floor phone", "3C21"),
                ),
                onCancelClicked = {},
                bluetoothUnavailable = WooPosCardReaderConnectionState.BluetoothUnavailable(
                    requirement = WooPosCardReaderConnectionState.BluetoothRequirement.Unmet.BluetoothOff,
                    onFixClicked = {},
                ),
            ),
            onBackPressed = {},
            onDismiss = {},
        )
    }
}

private fun btReader(id: String) = WooPosCardReaderConnectionState.FoundReader(
    id = id,
    name = id,
    transport = WooPosDiscoveryTransport.Bluetooth,
    fingerprintSuffix = null,
    onConnectClicked = {},
)

private fun phoneReader(name: String, suffix: String) = WooPosCardReaderConnectionState.FoundReader(
    id = "phone-$suffix",
    name = name,
    transport = WooPosDiscoveryTransport.WifiLan,
    fingerprintSuffix = suffix,
    onConnectClicked = {},
)

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

@WooPosPreview
@Composable
fun WooPosCardReaderConnectionDialogOnboardingErrorPreview() {
    WooPosTheme {
        WooPosCardReaderConnectionDialogContent(
            isVisible = true,
            state = WooPosCardReaderConnectionState.OnboardingError(
                title = "Your account has pending requirements",
                message = "There are pending requirements in your account." +
                    " Please complete those requirements to keep accepting In-Person Payments.",
                primaryButton = WooPosCardReaderConnectionState.OnboardingError.PrimaryButton(
                    label = "Skip",
                    onClick = {},
                ),
                onDismissClicked = {},
            ),
            onBackPressed = {},
            onDismiss = {},
        )
    }
}
