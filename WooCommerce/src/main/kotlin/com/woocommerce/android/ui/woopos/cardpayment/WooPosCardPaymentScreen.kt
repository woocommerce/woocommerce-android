package com.woocommerce.android.ui.woopos.cardpayment

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularLoadingIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSuccessCheckmark
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSuccessCheckmarkAnimationStage
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
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

    BackHandler { viewModel.onBackClicked() }

    WooPosCardPaymentScreenContent(
        state = state,
        showCashPaymentButton = viewModel.showCashPaymentButton,
        onRetryClicked = viewModel::onRetryClicked,
        onDismissClicked = viewModel::onDismissClicked,
        onConnectReaderClicked = viewModel::onConnectReaderClicked,
        onDoneClicked = viewModel::onDoneClicked,
        onEmailReceiptClicked = viewModel::onEmailReceiptClicked,
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
    onDoneClicked: () -> Unit,
    onEmailReceiptClicked: () -> Unit,
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

        StateChangeAnimated(visible = state is WooPosCardPaymentState.PaymentSuccess) {
            if (state is WooPosCardPaymentState.PaymentSuccess) {
                CardPaymentSuccess(
                    state = state,
                    onDoneClicked = onDoneClicked,
                    onEmailReceiptClicked = onEmailReceiptClicked,
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
        WooPosCircularLoadingIndicator(modifier = Modifier.size(160.dp))
    }
}

@Composable
private fun CardPaymentPreparingReader(
    state: WooPosCardPaymentState.Collecting.Preparing,
    showCashPaymentButton: Boolean,
    onCashPaymentClicked: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            WooPosCircularLoadingIndicator(modifier = Modifier.size(160.dp))
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
        CashPaymentButton(
            showCashPaymentButton = showCashPaymentButton,
            onCashPaymentClicked = onCashPaymentClicked,
        )
    }
}

@Composable
private fun CardPaymentReadyForPayment(
    state: WooPosCardPaymentState.Collecting.ReadyForPayment,
    showCashPaymentButton: Boolean,
    onCashPaymentClicked: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val tapCardAnimation by rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.woopos_card_ilustration)
            )
            LottieAnimation(
                modifier = Modifier.size(256.dp),
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
        CashPaymentButton(
            showCashPaymentButton = showCashPaymentButton,
            onCashPaymentClicked = onCashPaymentClicked,
        )
    }
}

@Composable
private fun BoxScope.CashPaymentButton(
    showCashPaymentButton: Boolean,
    onCashPaymentClicked: () -> Unit,
) {
    if (showCashPaymentButton) {
        WooPosOutlinedButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = WooPosSpacing.Huge.value)
                .height(80.dp)
                .width(604.dp),
            text = stringResource(R.string.woopos_cash_payment_title),
            onClick = onCashPaymentClicked,
        )
    }
}

@Composable
private fun CardPaymentReaderDisconnected(
    state: WooPosCardPaymentState.Collecting.ReaderDisconnected,
    onConnectReaderClicked: () -> Unit,
    showCashPaymentButton: Boolean,
    onCashPaymentClicked: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(WooPosSpacing.XLarge.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Image(
                modifier = Modifier.size(140.dp),
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

            Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))

            WooPosButton(
                text = state.actionButtonLabel,
                onClick = onConnectReaderClicked,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(80.dp)
            )
        }
        CashPaymentButton(
            showCashPaymentButton = showCashPaymentButton,
            onCashPaymentClicked = onCashPaymentClicked,
        )
    }
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
                modifier = Modifier.size(256.dp),
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = WooPosSpacing.Huge.value),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(WooPosSpacing.Huge.value))
        Icon(
            modifier = Modifier.size(84.dp),
            imageVector = WooPosIcons.ErrorX,
            contentDescription = stringResource(id = R.string.woopos_error_icon_content_description),
            tint = WooPosTheme.colors.unspecified,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))
        WooPosText(
            text = state.title,
            style = WooPosTypography.BodyXLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
        WooPosText(
            text = state.subtitle,
            style = WooPosTypography.BodyLarge,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))
        WooPosButton(
            text = state.retryButtonLabel,
            modifier = Modifier
                .height(80.dp)
                .width(604.dp),
            onClick = onRetryClicked,
        )
        if (state.isDismissButtonVisible) {
            Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))
            WooPosOutlinedButton(
                modifier = Modifier.width(604.dp),
                text = stringResource(R.string.woo_pos_payment_failed_go_back_to_checkout),
                onClick = onDismissClicked,
            )
        }
        Spacer(modifier = Modifier.height(WooPosSpacing.Huge.value))
    }
}

@Suppress("DestructuringDeclarationWithTooManyEntries")
@Composable
private fun CardPaymentSuccess(
    state: WooPosCardPaymentState.PaymentSuccess,
    onDoneClicked: () -> Unit,
    onEmailReceiptClicked: () -> Unit,
) {
    val animationStage = remember { mutableStateOf(WooPosSuccessCheckmarkAnimationStage.INITIAL) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceBright),
        contentAlignment = Alignment.Center
    ) {
        val hugeSpacing = WooPosSpacing.Huge.value
        val mediumSpacing = WooPosSpacing.Medium.value
        val marginBetweenButtonAndText by animateDpAsState(
            targetValue = if (animationStage.value >= WooPosSuccessCheckmarkAnimationStage.BUTTONS) {
                hugeSpacing
            } else {
                mediumSpacing
            },
            label = "Check mark size"
        )
        val checkMarkIconMargin = WooPosSpacing.XXXLarge.value
        val textsMargin = WooPosSpacing.Small.value

        ConstraintLayout {
            val (icon, title, message, buttonDone, buttonEmailReceipts) = createRefs()

            WooPosSuccessCheckmark(
                contentDescription = stringResource(R.string.woopos_payment_successful_label),
                onAnimationStageChanged = { stage -> animationStage.value = stage },
                modifier = Modifier
                    .constrainAs(icon) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(title.top, margin = checkMarkIconMargin)
                    }
            )

            WooPosText(
                text = stringResource(R.string.woopos_payment_successful_label),
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.constrainAs(title) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(message.top, margin = textsMargin)
                }
            )

            WooPosText(
                text = state.orderTotalText,
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.constrainAs(message) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(buttonDone.top, margin = marginBetweenButtonAndText)
                }
            )

            val marginBetweenButtons = WooPosSpacing.Medium.value
            WooPosButton(
                modifier = Modifier
                    .constrainAs(buttonDone) {
                        bottom.linkTo(buttonEmailReceipts.top, margin = marginBetweenButtons)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .height(80.dp)
                    .width(604.dp),
                onClick = onDoneClicked,
                text = stringResource(R.string.woopos_card_payment_done_button)
            )

            WooPosOutlinedButton(
                modifier = Modifier
                    .constrainAs(buttonEmailReceipts) {
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .height(80.dp)
                    .width(604.dp),
                onClick = onEmailReceiptClicked,
                text = stringResource(R.string.woopos_receipt_button)
            )
        }
    }
}
