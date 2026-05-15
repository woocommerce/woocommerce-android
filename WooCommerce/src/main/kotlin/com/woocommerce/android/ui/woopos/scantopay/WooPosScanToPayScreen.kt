package com.woocommerce.android.ui.woopos.scantopay

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularLoadingIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosQrCode
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
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
        onCancelClicked = { viewModel.onBackClicked() },
        onRetryClicked = { viewModel.onUIEvent(WooPosScanToPayUIEvent.RetryClicked) },
    )
    BackHandler { viewModel.onBackClicked() }
}

@Composable
private fun WooPosScanToPayScreen(
    state: WooPosScanToPayState,
    onCancelClicked: () -> Unit,
    onRetryClicked: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        WooPosToolbar(
            titleText = stringResource(R.string.woopos_scan_to_pay_title),
            onBackClicked = onCancelClicked,
        )
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (state) {
                WooPosScanToPayState.Loading,
                WooPosScanToPayState.PaymentDetected -> WooPosCircularLoadingIndicator()

                is WooPosScanToPayState.ShowingQR -> ShowingQR(state = state, onCancelClicked = onCancelClicked)

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
        if (state.totalText.isNotBlank()) {
            WooPosText(
                text = state.totalText,
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))
        }
        WooPosText(
            text = stringResource(R.string.woopos_scan_to_pay_subtitle),
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))
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
        if (state.retryable) {
            WooPosButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.woopos_scan_to_pay_retry),
                onClick = onRetryClicked,
            )
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
        }
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
    DisposableEffect(active) {
        val previous = window.attributes.screenBrightness
        if (active) {
            window.attributes = window.attributes.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            }
        }
        onDispose {
            window.attributes = window.attributes.apply { screenBrightness = previous }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
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
                retryable = true,
            ),
            onCancelClicked = {},
            onRetryClicked = {},
        )
    }
}
