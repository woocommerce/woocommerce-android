package com.woocommerce.android.ui.woopos.home.scanningsetup

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
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.BarcodeReaderDevice
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.ScanningSetupStep
import com.woocommerce.android.util.ChromeCustomTabUtils

@Composable
fun WooPosScanningSetupDialog(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    onShowBarcodeInfoDialog: () -> Unit = {},
) {
    val viewModel = hiltViewModel<WooPosScanningSetupViewModel>()
    val context = LocalContext.current

    LaunchedEffect(isVisible) {
        viewModel.resetToWelcomeState()
    }

    LaunchedEffect(Unit) {
        viewModel.openUrlEvent.collect { url ->
            ChromeCustomTabUtils.launchUrl(context, url, enableSlideAnimation = true)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.showBarcodeInfoDialogEvent.collect {
            onShowBarcodeInfoDialog()
        }
    }
    WooPosDialogWrapper(
        isVisible = isVisible,
        onDismissRequest = onDismissRequest,
        dialogBackgroundContentDescription = "Scanner setup dialog"
    ) {
        val state by viewModel.state.collectAsState()
        Box(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.surfaceBright)
                .padding(WooPosSpacing.XLarge.value.toAdaptivePadding())
        ) {
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "step_transition",
                modifier = Modifier.padding(top = WooPosSpacing.XLarge.value.toAdaptivePadding()),
            ) { step ->
                when (step) {
                    is ScanningSetupStep.Welcome -> WelcomeContent(
                        step = step,
                        onBluetoothSelected = {
                            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnBluetoothScannerSelected)
                        },
                        onViewDocumentation = {
                            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnViewDocumentation)
                        }
                    )

                    is ScanningSetupStep.DeviceSelection -> DeviceSelectionContent(
                        step = step,
                        selectedDevice = state.selectedDevice,
                        onDeviceSelected = { device ->
                            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(device))
                        },
                    )

                    is ScanningSetupStep.Introduction -> IntroductionContent(
                        step = step,
                        onPrimaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked) },
                        onSecondaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnSecondaryButtonClicked) }
                    )

                    is ScanningSetupStep.BluetoothWarning -> BluetoothWarningContent(
                        step = step,
                        onPrimaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked) },
                        onSecondaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnSecondaryButtonClicked) }
                    )

                    is ScanningSetupStep.BluetoothPairing -> BarcodeStepContent(
                        step = step,
                        onPrimaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked) },
                        onSecondaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnSecondaryButtonClicked) }
                    )

                    is ScanningSetupStep.PairOnYourDevice -> BarcodeStepContent(
                        step = step,
                        onPrimaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked) },
                        onSecondaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnSecondaryButtonClicked) }
                    )

                    is ScanningSetupStep.TestYourScanner -> BarcodeStepContent(
                        step = step,
                        onPrimaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked) },
                        onSecondaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnSecondaryButtonClicked) }
                    )

                    is ScanningSetupStep.ScannerSetupComplete -> SetupCompleteContent(
                        step = step,
                        onDone = onDismissRequest
                    )
                }
            }

            IconButton(
                onClick = onDismissRequest,
                modifier = Modifier
                    .align(Alignment.TopEnd)
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
    }
}

@Composable
private fun WelcomeContent(
    step: ScanningSetupStep.Welcome,
    onBluetoothSelected: () -> Unit,
    onViewDocumentation: () -> Unit,
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
            modifier = Modifier.padding(bottom = WooPosSpacing.XLarge.value.toAdaptivePadding())
        )

        WooPosOutlinedButton(
            onClick = onBluetoothSelected,
            text = step.setupButtonText,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))

        WooPosOutlinedButton(
            onClick = onViewDocumentation,
            text = step.documentationButtonText,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun IntroductionContent(
    step: ScanningSetupStep.Introduction,
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
            modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value.toAdaptivePadding())
        )

        WooPosText(
            text = step.message,
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WooPosSpacing.XLarge.value.toAdaptivePadding())
        )

        SetupButtonsRow(
            primaryButtonText = step.primaryButtonText,
            secondaryButtonText = step.secondaryButtonText,
            onPrimaryClick = onPrimaryClick,
            onSecondaryClick = onSecondaryClick
        )
    }
}

@Composable
private fun BluetoothWarningContent(
    step: ScanningSetupStep.BluetoothWarning,
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
            modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value.toAdaptivePadding())
        )

        WooPosText(
            text = step.message,
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WooPosSpacing.XLarge.value.toAdaptivePadding())
        )

        SetupButtonsRow(
            primaryButtonText = step.primaryButtonText,
            secondaryButtonText = step.secondaryButtonText,
            onPrimaryClick = onPrimaryClick,
            onSecondaryClick = onSecondaryClick
        )
    }
}

@Composable
private fun BarcodeStepContent(
    step: ScanningSetupStep,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
) {
    val title: String
    val message: String
    val barcodeRes: Int
    val instruction: String
    val primaryText: String
    val secondaryText: String

    when (step) {
        is ScanningSetupStep.BluetoothPairing -> {
            title = step.title
            message = step.message
            barcodeRes = step.barcodeImageRes
            instruction = step.instructionText
            primaryText = step.primaryButtonText
            secondaryText = step.secondaryButtonText
        }

        is ScanningSetupStep.PairOnYourDevice -> {
            title = step.title
            message = step.message
            barcodeRes = step.barcodeImageRes
            instruction = step.instructionText
            primaryText = step.primaryButtonText
            secondaryText = step.secondaryButtonText
        }

        is ScanningSetupStep.TestYourScanner -> {
            title = step.title
            message = step.message
            barcodeRes = step.barcodeImageRes
            instruction = step.instructionText
            primaryText = step.primaryButtonText
            secondaryText = step.secondaryButtonText
        }

        else -> error("Invalid step type for BarcodeStepContent: $step")
    }

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
                painter = painterResource(id = barcodeRes),
                contentDescription = "Barcode",
                modifier = Modifier.fillMaxSize()
            )
        }

        WooPosText(
            text = instruction,
            style = WooPosTypography.BodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                top = WooPosSpacing.Large.value.toAdaptivePadding(),
                bottom = WooPosSpacing.XLarge.value.toAdaptivePadding()
            )
        )

        SetupButtonsRow(
            primaryButtonText = primaryText,
            secondaryButtonText = secondaryText,
            onPrimaryClick = onPrimaryClick,
            onSecondaryClick = onSecondaryClick
        )
    }
}

@Composable
private fun SetupCompleteContent(
    step: ScanningSetupStep.ScannerSetupComplete,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SetupCompleteCheckIcon(
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

        WooPosButton(
            onClick = onDone,
            text = step.primaryButtonText,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DeviceSelectionContent(
    step: ScanningSetupStep.DeviceSelection,
    selectedDevice: BarcodeReaderDevice?,
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
            text = "Select a model from the list:",
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
                    isSelected = selectedDevice == device,
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
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WooPosCornerRadius.Medium.value))
            .border(
                width = if (isSelected) 4.dp else 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(WooPosCornerRadius.Medium.value)
            )
            .clickable { onClick() }
            .padding(WooPosSpacing.Large.value.toAdaptivePadding()),
        contentAlignment = Alignment.Center
    ) {
        WooPosText(
            text = device.displayName,
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
private fun SetupCompleteCheckIcon(
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(166.dp)
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
            modifier = Modifier.size(72.dp)
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
            WelcomeContent(
                step = ScanningSetupStep.Welcome(
                    title = "Start using a barcode scanner",
                    message = "Choose the type of scanner you'd like to connect",
                    setupButtonText = "Set up a barcode scanner",
                    documentationButtonText = "View barcode scanner documentation"
                ),
                onBluetoothSelected = {},
                onViewDocumentation = {}
            )
        }
    }
}
