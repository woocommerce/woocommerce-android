package com.woocommerce.android.ui.woopos.home.scanningsetup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.preview.FontScalePreviews
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.WooPosHomeState
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.ScanningSetupStep
import com.woocommerce.android.util.ChromeCustomTabUtils

@Composable
fun WooPosScanningSetupDialog(
    outerState: WooPosHomeState.ScanningSetupDialog,
    onDismissRequest: () -> Unit,
) {
    val viewModel = hiltViewModel<WooPosScanningSetupViewModel>()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.openUrlEvent.collect { url ->
            ChromeCustomTabUtils.launchUrl(context, url, enableSlideAnimation = true)
        }
    }

    WooPosDialogWrapper(
        isVisible = outerState.isVisible,
        onDismissRequest = onDismissRequest,
        dialogBackgroundContentDescription = "Scanner setup dialog"
    ) {
        val state by viewModel.state.collectAsState()
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceBright,
                    shape = RoundedCornerShape(WooPosCornerRadius.Large.value)
                )
                .padding(WooPosSpacing.XLarge.value.toAdaptivePadding())
        ) {
            IconButton(
                onClick = { onDismissRequest() },
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
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "step_transition"
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value.toAdaptivePadding())
        ) {
            WooPosOutlinedButton(
                onClick = onSecondaryClick,
                text = step.secondaryButtonText,
                modifier = Modifier.weight(1f)
            )

            WooPosButton(
                onClick = onPrimaryClick,
                text = step.primaryButtonText,
                modifier = Modifier.weight(1f)
            )
        }
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value.toAdaptivePadding())
        ) {
            WooPosOutlinedButton(
                onClick = onSecondaryClick,
                text = step.secondaryButtonText,
                modifier = Modifier.weight(1f)
            )

            WooPosButton(
                onClick = onPrimaryClick,
                text = step.primaryButtonText,
                modifier = Modifier.weight(1f)
            )
        }
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
        else -> return
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value.toAdaptivePadding())
        ) {
            WooPosOutlinedButton(
                onClick = onSecondaryClick,
                text = secondaryText,
                modifier = Modifier.weight(1f)
            )

            WooPosButton(
                onClick = onPrimaryClick,
                text = primaryText,
                modifier = Modifier.weight(1f)
            )
        }
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
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = WooPosSpacing.Large.value.toAdaptivePadding()),
            tint = MaterialTheme.colorScheme.primary
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
