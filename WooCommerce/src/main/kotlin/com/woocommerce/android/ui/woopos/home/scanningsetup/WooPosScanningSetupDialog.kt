package com.woocommerce.android.ui.woopos.home.scanningsetup

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.preview.FontScalePreviews
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButtonSmall
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.BarcodeReaderDevice
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.ScanningSetupStep
import com.woocommerce.android.util.WooLog

@Composable
fun WooPosScanningSetupDialog(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    onShowBarcodeInfoDialog: () -> Unit = {},
) {
    val viewModel = hiltViewModel<WooPosScanningSetupViewModel>()
    val context = LocalContext.current

    LaunchedEffect(isVisible) {
        viewModel.resetToInitialState()
    }

    LaunchedEffect(Unit) {
        viewModel.showBarcodeInfoDialogEvent.collect {
            onShowBarcodeInfoDialog()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.openBluetoothSettingsEvent.collect {
            try {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                WooLog.e(WooLog.T.POS, "Bluetooth settings activity not found.", e)
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.dismissDialogEvent.collect {
            onDismissRequest()
        }
    }

    WooPosDialogWrapper(
        isVisible = isVisible,
        onDismissRequest = onDismissRequest,
        dialogBackgroundContentDescription = stringResource(
            id = R.string.woopos_scanning_setup_dialog_content_description
        )
    ) {
        val state by viewModel.state.collectAsState()
        Column(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.surfaceBright)
                .padding(WooPosSpacing.XLarge.value.toAdaptivePadding())
        ) {
            Row {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(
                            id = R.string.woopos_exit_dialog_confirmation_close_content_description
                        ),
                        modifier = Modifier
                            .size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.size(WooPosSpacing.XLarge.value.toAdaptivePadding()))

            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "step_transition",
            ) { step ->
                when (step) {
                    is ScanningSetupStep.DeviceSelection -> DeviceSelectionContent(
                        step = step,
                        onDeviceSelected = { device ->
                            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(device))
                        },
                    )

                    is ScanningSetupStep.ScannerHIDModeSetup -> ScannerModeSetupContent(
                        title = step.title,
                        message = step.message,
                        qrCodeImageRes = step.qrCodeImageRes,
                        primaryButtonText = step.primaryButtonText,
                        secondaryButtonText = step.secondaryButtonText,
                        onPrimaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked) },
                        onSecondaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnSecondaryButtonClicked) }
                    )

                    is ScanningSetupStep.ScannerPairModeSetup -> ScannerModeSetupContent(
                        title = step.title,
                        message = step.message,
                        qrCodeImageRes = step.qrCodeImageRes,
                        primaryButtonText = step.primaryButtonText,
                        secondaryButtonText = step.secondaryButtonText,
                        onPrimaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked) },
                        onSecondaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnSecondaryButtonClicked) }
                    )

                    is ScanningSetupStep.PairYourScanner -> PairYourScannerContent(
                        step = step,
                        onPrimaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked) },
                        onSecondaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnSecondaryButtonClicked) },
                        onOpenBluetoothSettings = {
                            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnOpenBluetoothSettings)
                        }
                    )

                    is ScanningSetupStep.TestYourScanner -> TestYourScannerContent(
                        step = step,
                        onSecondaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnSecondaryButtonClicked) }
                    )

                    is ScanningSetupStep.ScannerSetupSuccess -> ScannerSetupSuccessContent(
                        step = step,
                        onSecondaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnSecondaryButtonClicked) }
                    )

                    is ScanningSetupStep.ScannerSetupInfo -> ScannerSetupInfoContent(
                        step = step,
                        onPrimaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked) },
                        onSecondaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnSecondaryButtonClicked) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScannerModeSetupContent(
    title: String,
    message: String,
    @DrawableRes qrCodeImageRes: Int,
    primaryButtonText: String,
    secondaryButtonText: String,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WooPosText(
            text = title,
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value.toAdaptivePadding())
        )

        WooPosText(
            text = message,
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WooPosSpacing.Large.value.toAdaptivePadding())
        )

        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(WooPosCornerRadius.Medium.value))
                .background(Color.White)
                .padding(WooPosSpacing.Medium.value.toAdaptivePadding()),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = qrCodeImageRes),
                contentDescription = stringResource(
                    id = R.string.woopos_scanning_setup_barcode_content_description
                ),
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))

        SetupButtonsRow(
            primaryButtonText = primaryButtonText,
            secondaryButtonText = secondaryButtonText,
            onPrimaryClick = onPrimaryClick,
            onSecondaryClick = onSecondaryClick
        )
    }
}

@Composable
private fun TestYourScannerContent(
    step: ScanningSetupStep.TestYourScanner,
    onSecondaryClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WooPosText(
            text = step.title,
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value.toAdaptivePadding())
        )

        WooPosText(
            text = step.message,
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WooPosSpacing.Large.value.toAdaptivePadding())
        )

        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(WooPosCornerRadius.Medium.value))
                .background(Color.White)
                .padding(WooPosSpacing.Medium.value.toAdaptivePadding()),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = step.barcodeImageRes),
                contentDescription = stringResource(
                    id = R.string.woopos_scanning_setup_barcode_content_description
                ),
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))

        WooPosOutlinedButton(
            onClick = onSecondaryClick,
            text = step.secondaryButtonText,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PairYourScannerContent(
    step: ScanningSetupStep.PairYourScanner,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = step.iconRes),
            contentDescription = null,
            modifier = Modifier
                .padding(WooPosSpacing.Medium.value.toAdaptivePadding()),
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value.toAdaptivePadding()))

        WooPosText(
            text = step.title,
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value.toAdaptivePadding()))

        WooPosText(
            text = step.message,
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.size(WooPosSpacing.XLarge.value.toAdaptivePadding()))

        WooPosOutlinedButtonSmall(
            text = step.bluetoothSettingsButtonText,
            onClick = onOpenBluetoothSettings,
        )

        Spacer(modifier = Modifier.size(WooPosSpacing.XLarge.value.toAdaptivePadding()))
        Spacer(modifier = Modifier.size(WooPosSpacing.XLarge.value.toAdaptivePadding()))

        SetupButtonsRow(
            primaryButtonText = step.primaryButtonText,
            secondaryButtonText = step.secondaryButtonText,
            onPrimaryClick = onPrimaryClick,
            onSecondaryClick = onSecondaryClick
        )
    }
}

@Composable
private fun DeviceSelectionContent(
    step: ScanningSetupStep.DeviceSelection,
    onDeviceSelected: (BarcodeReaderDevice) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WooPosText(
            text = step.title,
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value.toAdaptivePadding())
        )

        WooPosText(
            text = stringResource(id = R.string.woopos_scanning_setup_device_selection_message),
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WooPosSpacing.XLarge.value.toAdaptivePadding())
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value.toAdaptivePadding())
        ) {
            step.devices.forEach { device ->
                DeviceSelectionItem(
                    device = device,
                    onClick = {
                        onDeviceSelected(device)
                    }
                )
            }
        }
    }
}

@Composable
private fun DeviceSelectionItem(
    device: BarcodeReaderDevice,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WooPosCornerRadius.Medium.value))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.inverseSurface,
                shape = RoundedCornerShape(WooPosCornerRadius.Medium.value)
            )
            .clickable { onClick() }
            .padding(WooPosSpacing.Large.value.toAdaptivePadding()),
        contentAlignment = Alignment.Center
    ) {
        WooPosText(
            text = stringResource(device.displayNameRes),
            style = WooPosTypography.BodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SetupButtonsRow(
    primaryButtonText: String,
    secondaryButtonText: String,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
    primaryButtonState: WooPosButtonState = WooPosButtonState.ENABLED,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value.toAdaptivePadding())
    ) {
        WooPosOutlinedButton(
            onClick = onSecondaryClick,
            text = secondaryButtonText,
            modifier = Modifier.weight(1f)
        )

        WooPosButton(
            onClick = onPrimaryClick,
            text = primaryButtonText,
            modifier = Modifier.weight(1f),
            state = primaryButtonState
        )
    }
}

@Composable
private fun ScannerSetupSuccessContent(
    step: ScanningSetupStep.ScannerSetupSuccess,
    onSecondaryClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScannerSetupSuccessIcon(
            modifier = Modifier.padding(bottom = WooPosSpacing.Large.value.toAdaptivePadding())
        )

        WooPosText(
            text = step.title,
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value.toAdaptivePadding())
        )

        WooPosText(
            text = step.message,
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WooPosSpacing.XLarge.value.toAdaptivePadding())
        )

        WooPosOutlinedButton(
            onClick = onSecondaryClick,
            text = step.moreInfoButtonText,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ScannerSetupInfoContent(
    step: ScanningSetupStep.ScannerSetupInfo,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WooPosText(
            text = step.title,
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WooPosSpacing.XLarge.value.toAdaptivePadding())
        )

        WooPosText(
            text = step.message,
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(bottom = WooPosSpacing.XLarge.value.toAdaptivePadding())
                .fillMaxWidth()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = WooPosSpacing.Large.value.toAdaptivePadding()),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value.toAdaptivePadding())
        ) {
            step.bulletPoints.forEach { bulletPoint ->
                BulletPointItem(text = bulletPoint)
            }
        }

        WooPosText(
            text = step.infoText,
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = WooPosSpacing.XLarge.value.toAdaptivePadding())
        )

        SetupButtonsRow(
            primaryButtonText = step.doneButtonText,
            secondaryButtonText = step.backButtonText,
            onPrimaryClick = onPrimaryClick,
            onSecondaryClick = onSecondaryClick
        )
    }
}

@Composable
private fun BulletPointItem(text: String) {
    WooPosText(
        text = "• $text",
        style = WooPosTypography.BodyLarge,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ScannerSetupSuccessIcon(
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(88.dp)
            .shadow(
                elevation = WooPosElevation.Medium.value,
                shape = CircleShape,
                clip = false
            )
            .background(WooPosTheme.colors.success, CircleShape)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_woo_pos_check),
            tint = WooPosTheme.colors.onSuccess,
            contentDescription = stringResource(id = R.string.woopos_payment_successful_label),
            modifier = Modifier.size(40.dp)
        )
    }
}

@FontScalePreviews
@WooPosPreview
@Composable
fun WooPosScanningSetupDialogPreview() {
    WooPosTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            DeviceSelectionContent(
                step = ScanningSetupStep.DeviceSelection(
                    title = "Set up a barcode scanner",
                    devices = listOf(
                        BarcodeReaderDevice.TERA_1200,
                        BarcodeReaderDevice.STAR_BSH_20B,
                        BarcodeReaderDevice.INATECK_BLUETOOTH,
                        BarcodeReaderDevice.OTHER
                    )
                ),
                onDeviceSelected = {}
            )
        }
    }
}
