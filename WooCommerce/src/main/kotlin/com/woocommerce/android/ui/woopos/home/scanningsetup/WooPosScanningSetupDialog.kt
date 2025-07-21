package com.woocommerce.android.ui.woopos.home.scanningsetup

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.WooCommerce
import com.woocommerce.android.ui.compose.component.BarcodeEAN13Code
import com.woocommerce.android.ui.compose.component.getText
import com.woocommerce.android.ui.compose.preview.FontScalePreviews
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.ui.woopos.common.composeui.modifier.BarcodeInputDetector
import com.woocommerce.android.ui.woopos.common.composeui.modifier.listenForBarcodes
import com.woocommerce.android.ui.woopos.common.data.WOO_POS_BARCODE_DOC_URL
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.BarcodeReaderDevice
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.ScanningSetupStep
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.delay

@Composable
fun WooPosScanningSetupDialog(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
) {
    val viewModel = hiltViewModel<WooPosScanningSetupViewModel>()
    val context = LocalContext.current

    LaunchedEffect(isVisible) {
        if (isVisible) {
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDialogShown)
        }
    }

    val isClosing = remember { mutableStateOf(false) }

    LaunchedEffect(isClosing.value) {
        if (isClosing.value) {
            // Delay to allow the dialog to close before resetting state
            delay(300)
            viewModel.resetToInitialState()
            viewModel.stopScannerDetection()
            isClosing.value = false
        }
    }

    val onDismissRequestWrapper: () -> Unit = {
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDismissed)
        onDismissRequest()
        isClosing.value = true
    }

    LaunchedEffect(Unit) {
        viewModel.openBluetoothSettingsEvent.collect {
            try {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                (context.applicationContext as WooCommerce).appInitializer.get().crashLogging.sendReport(e)
                WooLog.e(WooLog.T.POS, "Bluetooth settings activity not found.", e)
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.dismissDialogEvent.collect {
            onDismissRequestWrapper()
        }
    }

    WooPosDialogWrapper(
        isVisible = isVisible,
        onDismissRequest = onDismissRequestWrapper,
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
                    onClick = onDismissRequestWrapper,
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
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = 250,
                            delayMillis = 200,
                            easing = FastOutSlowInEasing
                        )
                    ) togetherWith
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = 200,
                                easing = FastOutSlowInEasing
                            )
                        )
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
                        title = stringResource(step.titleRes),
                        message = stringResource(step.messageRes),
                        qrCodeImageRes = step.qrCodeImageRes,
                        primaryButtonText = stringResource(step.primaryButtonTextRes),
                        secondaryButtonText = stringResource(step.secondaryButtonTextRes),
                        onPrimaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked) },
                        onSecondaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnSecondaryButtonClicked) }
                    )

                    is ScanningSetupStep.ScannerPairModeSetup -> ScannerModeSetupContent(
                        title = stringResource(step.titleRes),
                        message = stringResource(step.messageRes),
                        qrCodeImageRes = step.qrCodeImageRes,
                        primaryButtonText = stringResource(step.primaryButtonTextRes),
                        secondaryButtonText = stringResource(step.secondaryButtonTextRes),
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

                    is ScanningSetupStep.TestYourScanner -> TestScannerContent(
                        title = stringResource(step.titleRes),
                        message = stringResource(step.messageRes),
                        barcodeValue = step.barcodeValue,
                        secondaryButtonText = stringResource(step.secondaryButtonTextRes),
                        onSecondaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnSecondaryButtonClicked) },
                        onBarcodeScanned = { barcodeResult ->
                            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnBarcodeScanned(barcodeResult))
                        }
                    )

                    is ScanningSetupStep.TestYourScannerTimeout -> TestScannerContent(
                        title = stringResource(step.titleRes),
                        message = stringResource(step.messageRes),
                        barcodeValue = step.barcodeValue,
                        secondaryButtonText = stringResource(step.secondaryButtonTextRes),
                        onSecondaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnSecondaryButtonClicked) },
                        onBarcodeScanned = { barcodeResult ->
                            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnBarcodeScanned(barcodeResult))
                        }
                    )

                    is ScanningSetupStep.ScannerSetupSuccess -> ScannerSetupSuccessContent(
                        step = step,
                        onPrimaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked) }
                    )

                    is ScanningSetupStep.ScannerSetupInfo -> ScannerSetupInfoContent(
                        step = step,
                        onPrimaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked) },
                        onSecondaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnSecondaryButtonClicked) }
                    )

                    is ScanningSetupStep.TestYourScannerScanFailed -> TestYourScannerScanFailedContent(
                        step = step,
                        onPrimaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked) },
                        onSecondaryClick = { viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnSecondaryButtonClicked) }
                    )

                    is ScanningSetupStep.ScannerSetupBarcodesOnProducts -> ScannerSetupBarcodesOnProductsContent(
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
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))
        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))

        Box(
            modifier = Modifier
                .size(172.dp)
                .clip(RoundedCornerShape(WooPosCornerRadius.Medium.value))
                .background(Color.White)
                .padding(WooPosSpacing.Small.value.toAdaptivePadding()),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = qrCodeImageRes),
                contentDescription = stringResource(
                    id = R.string.woopos_scanning_setup_barcode_content_description
                ),
                modifier = Modifier.size(168.dp)
            )
        }

        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))
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
private fun TestScannerContent(
    title: String,
    message: String,
    barcodeValue: String,
    secondaryButtonText: String,
    onSecondaryClick: () -> Unit,
    onBarcodeScanned: (BarcodeInputDetector.BarcodeResult) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .listenForBarcodes(onBarcodeScanned),
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

        BarcodeEAN13Code(
            barcodeValue,
            300.dp,
            150.dp,
            codeColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.surfaceBright
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))

        WooPosOutlinedButton(
            onClick = onSecondaryClick,
            text = secondaryButtonText,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TestYourScannerScanFailedContent(
    step: ScanningSetupStep.TestYourScannerScanFailed,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
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
            text = stringResource(step.titleRes),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value.toAdaptivePadding()))

        WooPosText(
            text = stringResource(step.messageRes),
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.size(WooPosSpacing.XLarge.value.toAdaptivePadding()))
        Spacer(modifier = Modifier.size(WooPosSpacing.XLarge.value.toAdaptivePadding()))

        SetupButtonsRow(
            primaryButtonText = stringResource(step.primaryButtonTextRes),
            secondaryButtonText = stringResource(step.secondaryButtonTextRes),
            onPrimaryClick = onPrimaryClick,
            onSecondaryClick = onSecondaryClick
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
            text = stringResource(step.titleRes),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value.toAdaptivePadding()))

        WooPosText(
            text = step.messageRes.getText(),
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.size(WooPosSpacing.Large.value.toAdaptivePadding()))

        WooPosText(
            text = stringResource(step.bluetoothSettingsButtonTextRes),
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, radius = 150.dp),
                    onClick = { onOpenBluetoothSettings() }
                )
                .padding(
                    horizontal = WooPosSpacing.Medium.value.toAdaptivePadding(),
                    vertical = WooPosSpacing.Small.value.toAdaptivePadding()
                )
        )

        Spacer(modifier = Modifier.size(WooPosSpacing.Large.value.toAdaptivePadding()))
        Spacer(modifier = Modifier.size(WooPosSpacing.XLarge.value.toAdaptivePadding()))

        SetupButtonsRow(
            primaryButtonText = stringResource(step.primaryButtonTextRes),
            secondaryButtonText = stringResource(step.secondaryButtonTextRes),
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
            text = stringResource(step.titleRes),
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
    onPrimaryClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScannerSetupSuccessIcon()

        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))

        WooPosText(
            text = stringResource(step.titleRes),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))

        WooPosText(
            text = stringResource(step.messageRes),
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))
        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))

        WooPosOutlinedButton(
            onClick = onPrimaryClick,
            text = stringResource(step.moreInfoButtonTextRes),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ScannerSetupSuccessIcon(
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(88.dp)
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

@Composable
private fun ScannerSetupBarcodesOnProductsContent(
    step: ScanningSetupStep.ScannerSetupBarcodesOnProducts,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WooPosText(
            text = stringResource(step.titleRes),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value.toAdaptivePadding())
        )

        val visitDocumentationText = stringResource(id = R.string.woopos_scanning_setup_visit_documentation)
        val linkAnnotation = LinkAnnotation.Url(
            WOO_POS_BARCODE_DOC_URL
        ) { urlAnnotation ->
            ChromeCustomTabUtils.launchUrl(
                context,
                WOO_POS_BARCODE_DOC_URL,
                enableSlideAnimation = true
            )
        }

        val annotatedText = buildAnnotatedString {
            append(stringResource(step.messageRes))
            append(" ")
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                withLink(linkAnnotation) {
                    append(visitDocumentationText)
                }
            }
            append(".")
        }

        WooPosText(
            text = annotatedText,
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(
                bottom = WooPosSpacing.Large.value.toAdaptivePadding(),
                start = WooPosSpacing.XLarge.value.toAdaptivePadding(),
                end = WooPosSpacing.XLarge.value.toAdaptivePadding(),
            )
        )

        Image(
            painter = painterResource(id = R.drawable.img_pos_inventory_setup),
            contentDescription = stringResource(
                id = R.string.woopos_scanning_setup_barcodes_on_products_image_description
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(WooPosCornerRadius.Medium.value))
                .border(
                    width = 1.dp,
                    color = WooPosTheme.colors.outlineVariant,
                    shape = RoundedCornerShape(WooPosCornerRadius.Medium.value)
                )
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))

        SetupButtonsRow(
            primaryButtonText = stringResource(step.doneButtonTextRes),
            secondaryButtonText = stringResource(step.backButtonTextRes),
            onPrimaryClick = onPrimaryClick,
            onSecondaryClick = onSecondaryClick
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
            text = stringResource(step.titleRes),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WooPosSpacing.XLarge.value.toAdaptivePadding())
        )

        WooPosText(
            text = stringResource(step.messageRes),
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .padding(bottom = WooPosSpacing.XLarge.value.toAdaptivePadding())
                .fillMaxWidth()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = WooPosSpacing.Large.value.toAdaptivePadding()),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value.toAdaptivePadding())
        ) {
            step.bulletPointsRes.forEach { bulletPointRes ->
                BulletPointItem(text = stringResource(bulletPointRes))
            }
        }

        WooPosText(
            text = stringResource(step.infoTextRes),
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = WooPosSpacing.XLarge.value.toAdaptivePadding())
        )

        SetupButtonsRow(
            primaryButtonText = stringResource(step.nextButtonTextRes),
            secondaryButtonText = stringResource(step.backButtonTextRes),
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
                step = ScanningSetupStep.DeviceSelection,
                onDeviceSelected = {}
            )
        }
    }
}

@WooPosPreview
@Composable
fun WooPosScanningSetupTestScannerStep() {
    WooPosTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            TestScannerContent(
                title = "Test your scanner",
                message = "Scan the barcode below to test your scanner.",
                barcodeValue = "123456789012",
                secondaryButtonText = "Skip",
                onSecondaryClick = {},
                onBarcodeScanned = {}
            )
        }
    }
}

@WooPosPreview
@Composable
fun WooPosScanningSetupTestScannerFailedStep() {
    WooPosTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            TestYourScannerScanFailedContent(
                step = ScanningSetupStep.TestYourScannerScanFailed,
                onPrimaryClick = {},
                onSecondaryClick = {},
            )
        }
    }
}
