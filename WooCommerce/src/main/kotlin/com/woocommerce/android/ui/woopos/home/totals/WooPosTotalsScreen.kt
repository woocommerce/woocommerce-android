package com.woocommerce.android.ui.woopos.home.totals

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.Button
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularLoadingIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState.Totals
import com.woocommerce.android.ui.woopos.home.totals.payment.failed.WooPosPaymentFailedScreen
import com.woocommerce.android.ui.woopos.home.totals.payment.inprogress.WooPosPaymentInProgressScreen
import com.woocommerce.android.ui.woopos.home.totals.payment.success.WooPosPaymentSuccessScreen

@Composable
fun WooPosTotalsScreen(modifier: Modifier = Modifier) {
    val viewModel: WooPosTotalsViewModel = hiltViewModel()
    val state = viewModel.state.collectAsState().value
    WooPosTotalsScreen(
        modifier = modifier,
        state = state,
        onUIEvent = viewModel::onUIEvent,
    )
}

@Composable
private fun WooPosTotalsScreen(
    modifier: Modifier = Modifier,
    state: WooPosTotalsViewState,
    onUIEvent: (WooPosTotalsUIEvent) -> Unit,
) {
    Box(modifier = modifier) {
        StateChangeAnimated(visible = state is WooPosTotalsViewState.Checkout) {
            if (state is WooPosTotalsViewState.Checkout) {
                TotalsLoaded(
                    state = state,
                    onUIEvent = onUIEvent,
                )
            }
        }

        StateChangeAnimated(visible = state is WooPosTotalsViewState.PaymentSuccess) {
            if (state is WooPosTotalsViewState.PaymentSuccess) {
                WooPosPaymentSuccessScreen(
                    state,
                    onReceiptClicked = { onUIEvent(WooPosTotalsUIEvent.OnStartReceiptFlowClicked) },
                    onNewTransactionClicked = { onUIEvent(WooPosTotalsUIEvent.OnNewTransactionClicked) }
                )
            }
        }

        StateChangeAnimated(visible = state is WooPosTotalsViewState.Loading) {
            if (state is WooPosTotalsViewState.Loading) {
                TotalsLoading()
            }
        }

        StateChangeAnimated(visible = state is WooPosTotalsViewState.Error) {
            if (state is WooPosTotalsViewState.Error) {
                TotalsErrorScreen(
                    errorMessage = state.message,
                    onUIEvent = onUIEvent
                )
            }
        }

        StateChangeAnimated(visible = state is WooPosTotalsViewState.PaymentInProgress) {
            if (state is WooPosTotalsViewState.PaymentInProgress) {
                WooPosPaymentInProgressScreen(state, onUIEvent)
            }
        }

        StateChangeAnimated(visible = state is WooPosTotalsViewState.PaymentFailed) {
            if (state is WooPosTotalsViewState.PaymentFailed) {
                WooPosPaymentFailedScreen(
                    state = state,
                    onUIEvent = onUIEvent,
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
private fun TotalsLoaded(
    state: WooPosTotalsViewState.Checkout,
    onUIEvent: (WooPosTotalsUIEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (!state.isFreeOrder) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when (val readerStatus = state.readerStatus) {
                    is WooPosTotalsViewState.ReaderStatus.Disconnected -> {
                        ReaderDisconnected(modifier = Modifier, status = readerStatus, onUIEvent = onUIEvent)
                    }

                    is WooPosTotalsViewState.ReaderStatus.Preparing,
                    is WooPosTotalsViewState.ReaderStatus.CheckingOrder -> {
                        PreparingReader(readerStatus)
                    }

                    is WooPosTotalsViewState.ReaderStatus.ReadyForPayment -> {
                        ReaderReadyForPayment(readerStatus)
                    }
                }
            }
        }

        AnimatedContent(
            targetState = state.totals,
            label = "totals_grid_animation",
        ) { state ->
            when (state) {
                is Totals.Hidden -> Unit
                is Totals.Visible -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = WooPosSpacing.XLarge.value.toAdaptivePadding(),
                                vertical = WooPosSpacing.Medium.value.toAdaptivePadding()
                            )
                            .weight(.9f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        TotalsGrid(totals = state)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))
                            WooPosOutlinedButton(
                                text = stringResource(R.string.woopos_payment_take_cash_payment_label),
                                onClick = { onUIEvent(WooPosTotalsUIEvent.OnCashPaymentClicked) },
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreparingReader(readerStatus: WooPosTotalsViewState.ReaderStatus) {
    WooPosCircularLoadingIndicator(modifier = Modifier.size(160.dp))
    Spacer(modifier = Modifier.height(WooPosSpacing.Large.value.toAdaptivePadding()))
    WooPosText(
        text = readerStatus.title,
        style = WooPosTypography.BodyLarge,
    )
    Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))
    WooPosText(
        text = readerStatus.subtitle,
        style = WooPosTypography.Heading,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ReaderReadyForPayment(readerStatus: WooPosTotalsViewState.ReaderStatus) {
    val tapCardAnimation by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.woopos_card_ilustration))
    LottieAnimation(
        modifier = Modifier.size(256.dp),
        composition = tapCardAnimation,
        clipSpec = LottieClipSpec.Markers("reader_awaiting_start", "reader_awaiting_end"),
        iterations = LottieConstants.IterateForever,
    )
    Spacer(modifier = Modifier.height(WooPosSpacing.Large.value.toAdaptivePadding()))
    WooPosText(
        text = readerStatus.title,
        style = WooPosTypography.BodyLarge,
    )
    Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))
    WooPosText(
        text = readerStatus.subtitle,
        style = WooPosTypography.Heading,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ReaderDisconnected(
    modifier: Modifier = Modifier,
    status: WooPosTotalsViewState.ReaderStatus.Disconnected,
    onUIEvent: (WooPosTotalsUIEvent) -> Unit,
) {
    Column(
        modifier = modifier.padding(WooPosSpacing.XLarge.value.toAdaptivePadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Icon(
            modifier = Modifier.size(122.dp),
            painter = painterResource(id = R.drawable.img_card_reader_not_connected_v2),
            contentDescription = stringResource(id = R.string.woopos_reader_not_connected_description),
            tint = WooPosTheme.colors.unspecified,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))

        WooPosText(
            text = status.title,
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))

        WooPosText(
            text = status.subtitle,
            style = WooPosTypography.BodyLarge,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))
        WooPosButton(
            text = status.actionButtonLabel,
            onClick = { onUIEvent(WooPosTotalsUIEvent.ConnectReaderClicked) },
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(80.dp)
        )
    }
}

@Composable
private fun TotalsGrid(totals: Totals.Visible) {
    Column(
        modifier = Modifier
            .padding(WooPosSpacing.Large.value.toAdaptivePadding())
            .fillMaxWidth(0.5f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TotalsGridRow(
            textOne = stringResource(R.string.woopos_payment_subtotal_label),
            textTwo = totals.orderSubtotalText,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))

        TotalsGridRow(
            textOne = stringResource(R.string.woopos_payment_tax_label),
            textTwo = totals.orderTaxText,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))

        TotalsGridRow(
            textOne = stringResource(R.string.woopos_payment_total_label),
            textTwo = totals.orderTotalText,
            styleOne = WooPosTypography.Heading,
            fontWeightOne = FontWeight.Bold,
            styleTwo = WooPosTypography.Heading,
            fontWeightTwo = FontWeight.Bold,
        )
    }
}

@Composable
private fun TotalsGridRow(
    textOne: String,
    textTwo: String,
    styleOne: WooPosTypography = WooPosTypography.BodyLarge,
    fontWeightOne: FontWeight = FontWeight.Normal,
    styleTwo: WooPosTypography = WooPosTypography.BodyLarge,
    fontWeightTwo: FontWeight = FontWeight.Normal,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        WooPosText(
            text = textOne,
            style = styleOne,
            fontWeight = fontWeightOne,
        )
        WooPosText(
            text = textTwo,
            style = styleTwo,
            fontWeight = fontWeightTwo,
        )
    }
}

@Composable
private fun TotalsLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            WooPosShimmerBox(
                modifier = Modifier
                    .height(WooPosSpacing.Large.value)
                    .width(332.dp)
                    .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Large.value.toAdaptivePadding()))

            WooPosShimmerBox(
                modifier = Modifier
                    .height(WooPosSpacing.Large.value)
                    .width(332.dp)
                    .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Large.value.toAdaptivePadding()))

            WooPosShimmerBox(
                modifier = Modifier
                    .height(40.dp)
                    .width(332.dp)
                    .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
            )
        }
    }
}

@Composable
private fun TotalsErrorScreen(
    errorMessage: String,
    onUIEvent: (WooPosTotalsUIEvent) -> Unit
) {
    WooPosErrorScreen(
        message = stringResource(R.string.woopos_totals_main_error_label),
        reason = errorMessage,
        primaryButton = Button(
            text = stringResource(R.string.retry),
            click = { onUIEvent(WooPosTotalsUIEvent.RetryOrderCreationClicked) }
        )
    )
}

@Composable
@WooPosPreview
fun WooPosTotalsScreenPreview(modifier: Modifier = Modifier) {
    WooPosTheme {
        WooPosTotalsScreen(
            modifier = modifier,
            state = WooPosTotalsViewState.Checkout(
                totals = Totals.Visible(
                    orderSubtotalText = "$420.00",
                    orderTotalText = "$462.00",
                    orderTaxText = "$42.00",
                ),
                readerStatus = WooPosTotalsViewState.ReaderStatus.ReadyForPayment(
                    title = "Ready for payment",
                    subtitle = "Tap, swipe or insert card"
                ),
                isFreeOrder = false
            ),
            onUIEvent = {},
        )
    }
}

@Composable
@WooPosPreview
fun WooPosTotalsScreenPreviewReaderNotConnected(modifier: Modifier = Modifier) {
    WooPosTheme {
        WooPosTotalsScreen(
            modifier = modifier,
            state = WooPosTotalsViewState.Checkout(
                totals = Totals.Visible(
                    orderSubtotalText = "$420.00",
                    orderTotalText = "$462.00",
                    orderTaxText = "$42.00",
                ),
                readerStatus = WooPosTotalsViewState.ReaderStatus.Disconnected(
                    title = "Reader not connected",
                    subtitle = "To process this payment, please connect your reader.",
                    actionButtonLabel = "Connect to a reader",
                ),
                isFreeOrder = false
            ),
            onUIEvent = {},
        )
    }
}

@Composable
@WooPosPreview
fun WooPosTotalsScreenPreviewWithCashPaymentAvailable() {
    WooPosTheme {
        WooPosTotalsScreen(
            modifier = Modifier,
            state = WooPosTotalsViewState.Checkout(
                totals = Totals.Visible(
                    orderSubtotalText = "$420.00",
                    orderTotalText = "$462.00",
                    orderTaxText = "$42.00",
                ),
                readerStatus = WooPosTotalsViewState.ReaderStatus.Disconnected(
                    title = "Reader not connected",
                    subtitle = "To process this payment, please connect your reader.",
                    actionButtonLabel = "Connect to a reader",
                ),
                isFreeOrder = false
            ),
            onUIEvent = {},
        )
    }
}

@Composable
@WooPosPreview
fun WooPosTotalsScreenPreviewForFreeOrders() {
    WooPosTheme {
        WooPosTotalsScreen(
            modifier = Modifier,
            state = WooPosTotalsViewState.Checkout(
                totals = Totals.Visible(
                    orderSubtotalText = "$420.00",
                    orderTotalText = "$462.00",
                    orderTaxText = "$42.00",
                ),
                readerStatus = WooPosTotalsViewState.ReaderStatus.Disconnected(
                    title = "Reader not connected",
                    subtitle = "To process this payment, please connect your reader.",
                    actionButtonLabel = "Connect to a reader",
                ),
                isFreeOrder = true
            ),
            onUIEvent = {},
        )
    }
}

@Composable
@WooPosPreview
fun TotalsErrorPreview() {
    val readerStatus = WooPosTotalsViewState.ReaderStatus.Disconnected(
        title = "Reader not connected",
        subtitle = "To process this payment, please connect your reader.",
        actionButtonLabel = "Connect to a reader",
    )
    WooPosTheme {
        ReaderDisconnected(modifier = Modifier, status = readerStatus) {}
    }
}

@Composable
@WooPosPreview
fun WooPosTotalsScreenLoadingPreview() {
    WooPosTheme {
        WooPosTotalsScreen(
            state = WooPosTotalsViewState.Loading,
            onUIEvent = {},
        )
    }
}

@Composable
@WooPosPreview
fun WooPosTotalsErrorScreenPreview() {
    WooPosTheme {
        TotalsErrorScreen(
            errorMessage = "An error occurred. Please try again.",
            onUIEvent = {}
        )
    }
}

@Composable
@WooPosPreview
fun PreparingReaderPReview() {
    val readerStatus = WooPosTotalsViewState.ReaderStatus.Preparing(
        title = "Getting ready",
        subtitle = "Preparing reader for payment"
    )
    WooPosTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PreparingReader(readerStatus)
        }
    }
}
