package com.woocommerce.android.ui.woopos.cardpayment

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularLoadingIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.adaptiveContentWidth
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptiveComponentSize
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

@Composable
fun WooPosCardPaymentScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
) {
    val viewModel: WooPosCardPaymentViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { onNavigationEvent(it) }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onScreenResumed()
                Lifecycle.Event.ON_PAUSE -> viewModel.onScreenPaused()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler { viewModel.onBackClicked() }

    WooPosCardPaymentScreenContent(
        state = state,
        showCashPaymentButton = viewModel.showCashPaymentButton,
        onRetryClicked = viewModel::onRetryClicked,
        onDismissClicked = viewModel::onDismissClicked,
        onConnectReaderClicked = viewModel::onConnectReaderClicked,
        onBackClicked = viewModel::onBackClicked,
        onCashPaymentClicked = viewModel::onCashPaymentClicked,
    )
}

@Composable
private fun WooPosCardPaymentScreenContent(
    state: WooPosCardPaymentState,
    showCashPaymentButton: Boolean,
    onRetryClicked: () -> Unit,
    onDismissClicked: () -> Unit,
    onConnectReaderClicked: () -> Unit,
    onBackClicked: () -> Unit,
    onCashPaymentClicked: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        StateChangeAnimated(visible = state is WooPosCardPaymentState.Initiating) {
            CardPaymentInitiating()
        }

        StateChangeAnimated(visible = state is WooPosCardPaymentState.Collecting.Preparing) {
            if (state is WooPosCardPaymentState.Collecting.Preparing) {
                CardPaymentPreparingReader(
                    state = state,
                    showCashPaymentButton = showCashPaymentButton,
                    onCashPaymentClicked = onCashPaymentClicked,
                )
            }
        }

        StateChangeAnimated(visible = state is WooPosCardPaymentState.Collecting.ReadyForPayment) {
            if (state is WooPosCardPaymentState.Collecting.ReadyForPayment) {
                CardPaymentReadyForPayment(
                    state = state,
                    showCashPaymentButton = showCashPaymentButton,
                    onCashPaymentClicked = onCashPaymentClicked,
                )
            }
        }

        StateChangeAnimated(visible = state is WooPosCardPaymentState.Collecting.ReaderDisconnected) {
            if (state is WooPosCardPaymentState.Collecting.ReaderDisconnected) {
                CardPaymentReaderDisconnected(
                    state = state,
                    onConnectReaderClicked = onConnectReaderClicked,
                    showCashPaymentButton = showCashPaymentButton,
                    onCashPaymentClicked = onCashPaymentClicked,
                    onBackClicked = onBackClicked,
                )
            }
        }

        StateChangeAnimated(visible = state is WooPosCardPaymentState.PaymentInProgress) {
            if (state is WooPosCardPaymentState.PaymentInProgress) {
                CardPaymentInProgress(state = state, onBackClicked = onBackClicked)
            }
        }

        StateChangeAnimated(visible = state is WooPosCardPaymentState.PaymentFailed) {
            if (state is WooPosCardPaymentState.PaymentFailed) {
                CardPaymentFailed(
                    state = state,
                    onRetryClicked = onRetryClicked,
                    onDismissClicked = onDismissClicked,
                    onBackClicked = onBackClicked,
                )
            }
        }
    }
}

@Composable
private fun StateChangeAnimated(
    visible: Boolean,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        content = content
    )
}

@Composable
private fun CardPaymentInitiating() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        WooPosCircularLoadingIndicator(modifier = Modifier.size(WooPosComponentSize.XLarge.value))
    }
}

@Composable
private fun CardPaymentPreparingReader(
    state: WooPosCardPaymentState.Collecting.Preparing,
    showCashPaymentButton: Boolean,
    onCashPaymentClicked: () -> Unit,
) {
    CardPaymentCenteredLayout(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(WooPosComponentSize.XXLarge.value),
                    contentAlignment = Alignment.Center,
                ) {
                    WooPosCircularLoadingIndicator(modifier = Modifier.size(WooPosComponentSize.XLarge.value))
                }
                Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))
                WooPosText(
                    text = state.title,
                    style = WooPosTypography.BodyLarge,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                )
                Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
                WooPosText(
                    text = state.subtitle,
                    style = WooPosTypography.Heading,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        summary = {
            CardPaymentOrderSummary(totals = state.orderTotals)
        },
        buttons = {
            if (showCashPaymentButton) {
                WooPosOutlinedButton(
                    modifier = Modifier
                        .height(WooPosComponentSize.Small.value)
                        .adaptiveContentWidth(),
                    text = stringResource(R.string.woopos_cash_payment_title),
                    onClick = onCashPaymentClicked,
                )
            }
        },
    )
}

@Composable
private fun CardPaymentReadyForPayment(
    state: WooPosCardPaymentState.Collecting.ReadyForPayment,
    showCashPaymentButton: Boolean,
    onCashPaymentClicked: () -> Unit,
) {
    CardPaymentCenteredLayout(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val tapCardAnimation by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(R.raw.woopos_card_ilustration)
                )
                LottieAnimation(
                    modifier = Modifier.size(WooPosComponentSize.XXLarge.value),
                    composition = tapCardAnimation,
                    clipSpec = LottieClipSpec.Markers("reader_awaiting_start", "reader_awaiting_end"),
                    iterations = LottieConstants.IterateForever,
                )
                Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))
                WooPosText(
                    text = state.title,
                    style = WooPosTypography.BodyLarge,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                )
                Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
                WooPosText(
                    text = state.subtitle,
                    style = WooPosTypography.Heading,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = WooPosSpacing.XLarge.value)
                )
            }
        },
        summary = {
            CardPaymentOrderSummary(totals = state.orderTotals)
        },
        buttons = {
            if (showCashPaymentButton) {
                WooPosOutlinedButton(
                    modifier = Modifier
                        .height(WooPosComponentSize.Small.value)
                        .adaptiveContentWidth(),
                    text = stringResource(R.string.woopos_cash_payment_title),
                    onClick = onCashPaymentClicked,
                )
            }
        },
    )
}

@Composable
private fun CardPaymentReaderDisconnected(
    state: WooPosCardPaymentState.Collecting.ReaderDisconnected,
    onConnectReaderClicked: () -> Unit,
    showCashPaymentButton: Boolean,
    onCashPaymentClicked: () -> Unit,
    onBackClicked: () -> Unit,
) {
    CardPaymentCenteredLayout(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        onBackClicked = onBackClicked,
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    modifier = Modifier.size(140.dp.toAdaptiveComponentSize()),
                    imageVector = WooPosIcons.CardReaderNotConnected,
                    contentDescription = stringResource(
                        id = R.string.woopos_reader_not_connected_description
                    ),
                )
                Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))
                WooPosText(
                    text = state.title,
                    style = WooPosTypography.Heading,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
                WooPosText(
                    text = state.subtitle,
                    style = WooPosTypography.BodyLarge,
                )
            }
        },
        summary = {
            CardPaymentOrderSummary(totals = state.orderTotals)
        },
        buttons = {
            WooPosButton(
                text = state.actionButtonLabel,
                onClick = onConnectReaderClicked,
                modifier = Modifier
                    .height(WooPosComponentSize.Small.value)
                    .adaptiveContentWidth()
            )
            if (showCashPaymentButton) {
                Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
                WooPosOutlinedButton(
                    modifier = Modifier
                        .height(WooPosComponentSize.Small.value)
                        .adaptiveContentWidth(),
                    text = stringResource(R.string.woopos_cash_payment_title),
                    onClick = onCashPaymentClicked,
                )
            }
        },
    )
}

@Composable
private fun CardPaymentInProgress(
    state: WooPosCardPaymentState.PaymentInProgress,
    onBackClicked: () -> Unit,
) {
    BackHandler { onBackClicked() }
    Box(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.primary)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.woopos_card_ilustration)
            )
            LottieAnimation(
                modifier = Modifier.size(WooPosComponentSize.XXLarge.value),
                composition = composition,
                iterations = LottieConstants.IterateForever,
                clipToCompositionBounds = false,
                clipSpec = LottieClipSpec.Markers("payment_processing_start", "payment_processing_end")
            )
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                WooPosText(
                    text = state.title,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = WooPosTypography.BodyLarge,
                )
                Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
                WooPosText(
                    text = state.subtitle,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = WooPosTypography.BodyXLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
    }
}

@Composable
private fun CardPaymentFailed(
    state: WooPosCardPaymentState.PaymentFailed,
    onRetryClicked: () -> Unit,
    onDismissClicked: () -> Unit,
    onBackClicked: () -> Unit,
) {
    BackHandler { onBackClicked() }
    CardPaymentCenteredLayout(
        onBackClicked = onBackClicked,
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    modifier = Modifier.size(WooPosComponentSize.Small.value),
                    imageVector = WooPosIcons.ErrorX,
                    contentDescription = stringResource(id = R.string.woopos_error_icon_content_description),
                    tint = WooPosTheme.colors.unspecified,
                )
                Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))
                WooPosText(
                    text = state.title,
                    style = WooPosTypography.BodyXLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
                WooPosText(
                    text = state.subtitle,
                    style = WooPosTypography.BodyLarge,
                )
            }
        },
        summary = null,
        buttons = {
            if (state.actionButtonLabel != null) {
                WooPosButton(
                    text = state.actionButtonLabel,
                    modifier = Modifier
                        .height(WooPosComponentSize.Small.value)
                        .adaptiveContentWidth(),
                    onClick = onRetryClicked,
                )
                Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))
            }
            if (state.isDismissButtonVisible) {
                WooPosOutlinedButton(
                    modifier = Modifier.adaptiveContentWidth(),
                    text = stringResource(R.string.woo_pos_payment_failed_go_back_to_checkout),
                    onClick = onDismissClicked,
                )
            }
        },
    )
}

@Composable
private fun CardPaymentCenteredLayout(
    modifier: Modifier = Modifier,
    onBackClicked: (() -> Unit)? = null,
    content: @Composable (() -> Unit),
    summary: @Composable (() -> Unit)?,
    buttons: @Composable (() -> Unit),
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (onBackClicked != null) {
            WooPosToolbar(
                titleText = stringResource(R.string.payment),
                onBackClicked = onBackClicked,
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .defaultMinSize(minHeight = maxHeight)
                    .padding(vertical = WooPosSpacing.XXXLarge.value),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                content()
                if (summary != null) {
                    summary()
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    buttons()
                }
            }
        }
    }
}

@Composable
private fun CardPaymentOrderSummary(
    totals: WooPosOrderTotalsViewState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(0.4f)
            .padding(WooPosSpacing.Large.value),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        OrderSummaryRow(
            label = stringResource(R.string.woopos_payment_subtotal_label),
            value = totals.subtotal,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        totals.discount?.let {
            OrderSummaryRow(
                label = stringResource(R.string.woopos_payment_discount_label),
                value = it,
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
        }

        OrderSummaryRow(
            label = stringResource(R.string.woopos_payment_tax_label),
            value = totals.taxes,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        OrderSummaryRow(
            label = stringResource(R.string.woopos_payment_total_label),
            value = totals.total,
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun OrderSummaryRow(
    label: String,
    value: String,
    style: WooPosTypography = WooPosTypography.BodyLarge,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        WooPosText(
            text = label,
            style = style,
            fontWeight = fontWeight,
        )
        WooPosText(
            text = value,
            style = style,
            fontWeight = fontWeight,
        )
    }
}

private fun previewOrderTotals() = WooPosOrderTotalsViewState(
    subtotal = "$10.00",
    discount = "-$2.00",
    taxes = "$1.50",
    total = "$9.50",
)

@WooPosPreview
@Composable
fun CardPaymentInitiatingPreview() {
    WooPosTheme {
        WooPosCardPaymentScreenContent(
            state = WooPosCardPaymentState.Initiating,
            showCashPaymentButton = false,
            onRetryClicked = {},
            onDismissClicked = {},
            onConnectReaderClicked = {},
            onBackClicked = {},
            onCashPaymentClicked = {},
        )
    }
}

@WooPosPreview
@Composable
fun CardPaymentPreparingReaderPreview() {
    WooPosTheme {
        WooPosCardPaymentScreenContent(
            state = WooPosCardPaymentState.Collecting.Preparing(
                title = "Preparing reader",
                subtitle = "$12.50",
                orderTotals = previewOrderTotals(),
            ),
            showCashPaymentButton = true,
            onRetryClicked = {},
            onDismissClicked = {},
            onConnectReaderClicked = {},
            onBackClicked = {},
            onCashPaymentClicked = {},
        )
    }
}

@WooPosPreview
@Composable
fun CardPaymentReadyForPaymentPreview() {
    WooPosTheme {
        WooPosCardPaymentScreenContent(
            state = WooPosCardPaymentState.Collecting.ReadyForPayment(
                title = "Ready for payment",
                subtitle = "Tap, insert, or swipe to pay $12.50",
                orderTotals = previewOrderTotals(),
            ),
            showCashPaymentButton = true,
            onRetryClicked = {},
            onDismissClicked = {},
            onConnectReaderClicked = {},
            onBackClicked = {},
            onCashPaymentClicked = {},
        )
    }
}

@WooPosPreview
@Composable
fun CardPaymentReaderDisconnectedPreview() {
    WooPosTheme {
        WooPosCardPaymentScreenContent(
            state = WooPosCardPaymentState.Collecting.ReaderDisconnected(
                title = "Reader disconnected",
                subtitle = "Please reconnect your card reader",
                actionButtonLabel = "Connect reader",
                orderTotals = previewOrderTotals(),
            ),
            showCashPaymentButton = true,
            onRetryClicked = {},
            onDismissClicked = {},
            onConnectReaderClicked = {},
            onBackClicked = {},
            onCashPaymentClicked = {},
        )
    }
}

@WooPosPreview
@Composable
fun CardPaymentInProgressPreview() {
    WooPosTheme {
        WooPosCardPaymentScreenContent(
            state = WooPosCardPaymentState.PaymentInProgress(
                title = "Processing payment",
                subtitle = "$12.50",
            ),
            showCashPaymentButton = false,
            onRetryClicked = {},
            onDismissClicked = {},
            onConnectReaderClicked = {},
            onBackClicked = {},
            onCashPaymentClicked = {},
        )
    }
}

@WooPosPreview
@Composable
fun CardPaymentFailedWithRetryPreview() {
    WooPosTheme {
        WooPosCardPaymentScreenContent(
            state = WooPosCardPaymentState.PaymentFailed(
                title = "Payment failed",
                subtitle = "Please try again",
                actionButtonLabel = "Try again",
                isDismissButtonVisible = true,
            ),
            showCashPaymentButton = false,
            onRetryClicked = {},
            onDismissClicked = {},
            onConnectReaderClicked = {},
            onBackClicked = {},
            onCashPaymentClicked = {},
        )
    }
}

@WooPosPreview
@Composable
fun CardPaymentFailedWithoutRetryPreview() {
    WooPosTheme {
        WooPosCardPaymentScreenContent(
            state = WooPosCardPaymentState.PaymentFailed(
                title = "Payment failed",
                subtitle = "Please try again",
                actionButtonLabel = null,
                isDismissButtonVisible = true,
            ),
            showCashPaymentButton = false,
            onRetryClicked = {},
            onDismissClicked = {},
            onConnectReaderClicked = {},
            onBackClicked = {},
            onCashPaymentClicked = {},
        )
    }
}
