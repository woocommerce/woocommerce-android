package com.woocommerce.android.ui.woopos.scantopay

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.extensions.findActivity
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularLoadingIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosQrCode
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.util.WooPosTestTags

@Composable
fun WooPosScanToPayScreen(onNavigationEvent: (WooPosNavigationEvent) -> Unit) {
    val viewModel = hiltViewModel<WooPosScanToPayViewModel>()
    val state = viewModel.state.collectAsState().value

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { onNavigationEvent(it) }
    }

    val isQrVisible = state is WooPosScanToPayState.ShowingQR
    MaxBrightnessWhen(active = isQrVisible)

    WooPosScanToPayScreen(
        state = state,
        onCancelClicked = { viewModel.onUIEvent(WooPosScanToPayUIEvent.CancelClicked) },
        onRetryClicked = { viewModel.onUIEvent(WooPosScanToPayUIEvent.RetryClicked) },
        onCollectOnRegisterClicked = { viewModel.onUIEvent(WooPosScanToPayUIEvent.CollectOnRegisterClicked) },
    )
    BackHandler(enabled = state !is WooPosScanToPayState.PaymentDetected) { viewModel.onBackClicked() }
}

@Composable
private fun WooPosScanToPayScreen(
    state: WooPosScanToPayState,
    onCancelClicked: () -> Unit,
    onRetryClicked: () -> Unit,
    onCollectOnRegisterClicked: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        WooPosToolbar(
            titleText = stringResource(R.string.woopos_scan_to_pay_title),
            onBackClicked = onCancelClicked,
        )
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (state) {
                // PaymentDetected briefly shows the same spinner as Loading while the VM emits
                // GoBack and the navigation pop is processed.
                WooPosScanToPayState.Loading,
                WooPosScanToPayState.PaymentDetected ->
                    WooPosCircularLoadingIndicator(
                        modifier = Modifier.size(WooPosComponentSize.XLarge.value),
                    )

                is WooPosScanToPayState.ShowingQR -> ShowingQR(state = state, onCancelClicked = onCancelClicked)

                WooPosScanToPayState.PayInPersonSelected -> PayInPersonSelected(
                    onCollectOnRegisterClicked = onCollectOnRegisterClicked,
                    onShowQrAgainClicked = onRetryClicked,
                )

                is WooPosScanToPayState.Failed -> Failed(
                    state = state,
                    onRetryClicked = onRetryClicked,
                    onCancelClicked = onCancelClicked,
                )
            }
        }
    }
}

@Composable
private fun ShowingQR(
    state: WooPosScanToPayState.ShowingQR,
    onCancelClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WooPosSpacing.Large.value),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_scan_to_pay_subtitle),
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Center,
        )
        if (state.totalText.isNotBlank()) {
            Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))
            WooPosText(
                text = state.totalText,
                style = WooPosTypography.BodyLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))
        @Suppress("WooPosDesignSystemComponentSizeUsageRule")
        WooPosQrCode(
            data = state.paymentUrl,
            size = 320.dp,
            modifier = Modifier.testTag(WooPosTestTags.SCAN_TO_PAY_QR_CODE),
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))
        WooPosOutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(WooPosTestTags.SCAN_TO_PAY_CANCEL_BUTTON),
            text = stringResource(R.string.woopos_scan_to_pay_cancel),
            onClick = onCancelClicked,
        )
    }
}

@Composable
private fun PayInPersonSelected(
    onCollectOnRegisterClicked: () -> Unit,
    onShowQrAgainClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WooPosSpacing.Large.value),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_scan_to_pay_pay_in_person_title),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
        WooPosText(
            text = stringResource(R.string.woopos_scan_to_pay_pay_in_person_message),
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))
        WooPosButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.woopos_scan_to_pay_pay_in_person_collect),
            onClick = onCollectOnRegisterClicked,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
        WooPosOutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.woopos_scan_to_pay_pay_in_person_show_qr),
            onClick = onShowQrAgainClicked,
        )
    }
}

@Composable
private fun Failed(
    state: WooPosScanToPayState.Failed,
    onRetryClicked: () -> Unit,
    onCancelClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WooPosSpacing.Large.value),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        WooPosText(
            text = state.message,
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))
        WooPosButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.woopos_scan_to_pay_retry),
            onClick = onRetryClicked,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
        WooPosOutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.woopos_scan_to_pay_cancel),
            onClick = onCancelClicked,
        )
    }
}

@Composable
private fun MaxBrightnessWhen(active: Boolean) {
    val window = LocalContext.current.findActivity()?.window ?: return
    // Capture the user's brightness once. Re-reading it inside DisposableEffect(active)
    // would observe our own FULL override on a true→false→true cycle and lock the app
    // to max brightness even after the screen closes.
    val originalBrightness = remember { window.attributes.screenBrightness }
    DisposableEffect(active) {
        if (active) {
            window.attributes = window.attributes.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            }
        }
        onDispose {
            window.attributes = window.attributes.apply { screenBrightness = originalBrightness }
        }
    }
}

@WooPosPreview
@Composable
private fun WooPosScanToPayLoadingPreview() {
    WooPosTheme {
        WooPosScanToPayScreen(
            state = WooPosScanToPayState.Loading,
            onCancelClicked = {},
            onRetryClicked = {},
            onCollectOnRegisterClicked = {},
        )
    }
}

@WooPosPreview
@Composable
private fun WooPosScanToPayShowingQrPreview() {
    WooPosTheme {
        WooPosScanToPayScreen(
            state = WooPosScanToPayState.ShowingQR(
                paymentUrl = "https://example.com/checkout/pay/abc123",
                totalText = "Order total: $42.00",
            ),
            onCancelClicked = {},
            onRetryClicked = {},
            onCollectOnRegisterClicked = {},
        )
    }
}

@WooPosPreview
@Composable
private fun WooPosScanToPayPayInPersonPreview() {
    WooPosTheme {
        WooPosScanToPayScreen(
            state = WooPosScanToPayState.PayInPersonSelected,
            onCancelClicked = {},
            onRetryClicked = {},
            onCollectOnRegisterClicked = {},
        )
    }
}

@WooPosPreview
@Composable
private fun WooPosScanToPayFailedPreview() {
    WooPosTheme {
        WooPosScanToPayScreen(
            state = WooPosScanToPayState.Failed(
                message = "Something went wrong. Please try again.",
            ),
            onCancelClicked = {},
            onRetryClicked = {},
            onCollectOnRegisterClicked = {},
        )
    }
}
