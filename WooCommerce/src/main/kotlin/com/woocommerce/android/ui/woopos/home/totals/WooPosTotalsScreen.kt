package com.woocommerce.android.ui.woopos.home.totals

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularLoadingIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreenButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.adaptiveContentWidth
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptiveComponentSize
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState.Totals
import com.woocommerce.android.ui.woopos.home.totals.payment.failed.WooPosPaymentFailedScreen
import com.woocommerce.android.ui.woopos.home.totals.payment.inprogress.WooPosPaymentInProgressScreen
import com.woocommerce.android.ui.woopos.home.totals.payment.success.WooPosPaymentSuccessScreen
import com.woocommerce.android.ui.woopos.util.WooPosTestTags

@Composable
fun WooPosTotalsScreen(
    modifier: Modifier = Modifier,
    viewModel: WooPosTotalsViewModel = hiltViewModel(),
) {
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

        StateChangeAnimated(visible = state is WooPosTotalsViewState.InvalidCouponError) {
            if (state is WooPosTotalsViewState.InvalidCouponError) {
                TotalsInvalidCouponsErrorScreen(
                    errorMessage = state.message,
                    errorReason = state.reason,
                    onUIEvent = onUIEvent
                )
            }
        }

        StateChangeAnimated(visible = state is WooPosTotalsViewState.ProductNotFoundError) {
            if (state is WooPosTotalsViewState.ProductNotFoundError) {
                TotalsProductNotFoundErrorScreen(
                    errorMessage = state.message,
                    errorReason = state.reason,
                    isRemoveProductSupported = state.isRemoveProductSupported,
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
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when (val readerStatus = state.readerStatus) {
                    is WooPosTotalsViewState.ReaderStatus.Disconnected -> {
                        ReaderDisconnected(status = readerStatus, onUIEvent = onUIEvent)
                    }
                    is WooPosTotalsViewState.ReaderStatus.Preparing -> {
                        PreparingReader(
                            title = readerStatus.title,
                            subtitle = readerStatus.subtitle
                        )
                    }
                    is WooPosTotalsViewState.ReaderStatus.CheckingOrder -> {
                        PreparingReader(
                            title = readerStatus.title,
                            subtitle = readerStatus.subtitle
                        )
                    }
                    is WooPosTotalsViewState.ReaderStatus.ReadyForPayment -> {
                        ReaderReadyForPayment(readerStatus)
                    }
                    WooPosTotalsViewState.ReaderStatus.Unavailable -> Unit
                }
            }

            AnimatedContent(
                targetState = state.totals,
                label = "totals_grid_animation"
            ) { totalsVisible ->
                if (totalsVisible is Totals.Visible) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = WooPosSpacing.XLarge.value,
                                vertical = WooPosSpacing.Medium.value,
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        TotalsGrid(totalsVisible)
                    }
                }
            }

            Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))
        }

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        WooPosOutlinedButton(
            text = stringResource(R.string.woopos_payment_take_cash_payment_label),
            onClick = { onUIEvent(WooPosTotalsUIEvent.OnCashPaymentClicked) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WooPosSpacing.XLarge.value)
                .padding(bottom = WooPosSpacing.XLarge.value)
                .testTag(WooPosTestTags.CASH_PAYMENT_BUTTON)
        )
    }
}

@Composable
private fun PreparingReader(title: String, subtitle: String) {
    WooPosCircularLoadingIndicator(modifier = Modifier.size(WooPosComponentSize.XLarge.value))
    Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))
    WooPosText(
        text = title,
        style = WooPosTypography.BodyLarge,
        color = WooPosTheme.colors.onSurfaceVariantHighest,
    )
    Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
    WooPosText(
        text = subtitle,
        style = WooPosTypography.Heading,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ReaderReadyForPayment(readerStatus: WooPosTotalsViewState.ReaderStatus.ReadyForPayment) {
    val tapCardAnimation by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.woopos_card_ilustration))
    LottieAnimation(
        modifier = Modifier.size(WooPosComponentSize.XXLarge.value),
        composition = tapCardAnimation,
        clipSpec = LottieClipSpec.Markers("reader_awaiting_start", "reader_awaiting_end"),
        iterations = LottieConstants.IterateForever,
    )
    Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))
    WooPosText(
        text = readerStatus.title,
        style = WooPosTypography.BodyLarge,
        color = WooPosTheme.colors.onSurfaceVariantHighest,
    )
    Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
    WooPosText(
        text = readerStatus.subtitle,
        style = WooPosTypography.Heading,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = WooPosSpacing.XLarge.value)
    )
}

@Composable
private fun ReaderDisconnected(
    modifier: Modifier = Modifier,
    status: WooPosTotalsViewState.ReaderStatus.Disconnected,
    onUIEvent: (WooPosTotalsUIEvent) -> Unit,
) {
    Column(
        modifier = modifier.padding(WooPosSpacing.XLarge.value),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Image(
            modifier = Modifier.size(140.dp.toAdaptiveComponentSize()),
            imageVector = WooPosIcons.CardReaderNotConnected,
            contentDescription = stringResource(id = R.string.woopos_reader_not_connected_description),
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))

        WooPosText(
            text = status.title,
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        WooPosText(
            text = status.subtitle,
            style = WooPosTypography.BodyLarge,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))
        WooPosButton(
            text = status.actionButtonLabel,
            onClick = { onUIEvent(WooPosTotalsUIEvent.ConnectReaderClicked) },
            modifier = Modifier
                .adaptiveContentWidth()
                .height(WooPosComponentSize.Small.value)
        )
    }
}

@Composable
private fun TotalsGrid(totals: Totals.Visible) {
    Column(
        modifier = Modifier
            .padding(WooPosSpacing.Large.value)
            .adaptiveContentWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TotalsGridRow(
            textOne = stringResource(R.string.woopos_payment_subtotal_label),
            textTwo = totals.orderSubtotalText,
            colorOne = WooPosTheme.colors.onSurfaceVariantHighest
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        totals.orderDiscountText?.let {
            TotalsGridRow(
                textOne = stringResource(R.string.woopos_payment_discount_label),
                textTwo = totals.orderDiscountText,
                colorOne = WooPosTheme.colors.onSurfaceVariantHighest,
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
        }

        TotalsGridRow(
            textOne = stringResource(R.string.woopos_payment_tax_label),
            textTwo = totals.orderTaxText,
            colorOne = WooPosTheme.colors.onSurfaceVariantHighest
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

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
    colorOne: Color = Color.Unspecified,
    styleTwo: WooPosTypography = WooPosTypography.BodyLarge,
    fontWeightTwo: FontWeight = FontWeight.Normal,
    colorTwo: Color = Color.Unspecified,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        WooPosText(
            text = textOne,
            style = styleOne,
            fontWeight = fontWeightOne,
            color = colorOne,
        )
        WooPosText(
            text = textTwo,
            style = styleTwo,
            fontWeight = fontWeightTwo,
            color = colorTwo,
        )
    }
}

@Composable
private fun TotalsLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = WooPosSpacing.XLarge.value),
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
                    .height(24.dp.toAdaptiveComponentSize())
                    .adaptiveContentWidth()
                    .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

            WooPosShimmerBox(
                modifier = Modifier
                    .height(24.dp.toAdaptiveComponentSize())
                    .adaptiveContentWidth()
                    .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

            WooPosShimmerBox(
                modifier = Modifier
                    .height(40.dp.toAdaptiveComponentSize())
                    .adaptiveContentWidth()
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
        primaryButton = WooPosErrorScreenButtonState(
            text = stringResource(R.string.retry),
            click = { onUIEvent(WooPosTotalsUIEvent.RetryOrderCreationClicked) }
        )
    )
}

@Composable
private fun TotalsInvalidCouponsErrorScreen(
    errorMessage: String,
    errorReason: String,
    onUIEvent: (WooPosTotalsUIEvent) -> Unit
) {
    return WooPosErrorScreen(
        message = errorMessage,
        reason = HtmlCompat.fromHtml(errorReason, HtmlCompat.FROM_HTML_MODE_COMPACT).toString(),
        primaryButton = WooPosErrorScreenButtonState(
            text = stringResource(R.string.woopos_totals_coupons_validation_failed_edit_order),
            click = { onUIEvent(WooPosTotalsUIEvent.GoBackToCheckoutAfterFailedCouponValidation) }
        ),
        secondaryButton = WooPosErrorScreenButtonState(
            text = stringResource(R.string.woopos_totals_coupons_validation_failed_remove_coupons),
            click = { onUIEvent(WooPosTotalsUIEvent.OnRemoveCouponsClicked) }
        )
    )
}

@Composable
private fun TotalsProductNotFoundErrorScreen(
    errorMessage: String,
    errorReason: String,
    isRemoveProductSupported: Boolean,
    onUIEvent: (WooPosTotalsUIEvent) -> Unit
) {
    return WooPosErrorScreen(
        message = errorMessage,
        reason = errorReason,
        primaryButton = WooPosErrorScreenButtonState(
            text = stringResource(R.string.woopos_totals_product_not_found_edit_order),
            click = { onUIEvent(WooPosTotalsUIEvent.GoBackToOrderEditAfterProductNotFound) }
        ),
        secondaryButton = if (isRemoveProductSupported) {
            WooPosErrorScreenButtonState(
                text = stringResource(R.string.woopos_totals_product_not_found_remove_products),
                click = { onUIEvent(WooPosTotalsUIEvent.OnRemoveProductsClicked) }
            )
        } else {
            null
        }
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
                    orderDiscountText = "$20.00",
                ),
                readerStatus = WooPosTotalsViewState.ReaderStatus.ReadyForPayment(
                    title = "Ready for payment",
                    subtitle = "Tap, swipe or insert card, Tap, swipe or insert card, Tap, swipe or insert card"
                ),
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
                    orderDiscountText = "$20.00",
                ),
                readerStatus = WooPosTotalsViewState.ReaderStatus.Disconnected(
                    title = "Reader not connected",
                    subtitle = "To process this payment, please connect your reader.",
                    actionButtonLabel = "Connect to a reader",
                ),
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
                    orderDiscountText = null,
                ),
                readerStatus = WooPosTotalsViewState.ReaderStatus.Disconnected(
                    title = "Reader not connected",
                    subtitle = "To process this payment, please connect your reader.",
                    actionButtonLabel = "Connect to a reader",
                ),
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
                    orderDiscountText = "$20.00",
                ),
                readerStatus = WooPosTotalsViewState.ReaderStatus.Unavailable,
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
fun TotalsCouponValidationFailedPreview() {
    WooPosTheme {
        TotalsInvalidCouponsErrorScreen(
            errorMessage = "Invalid coupons",
            errorReason = "Coupon '10OFF' is not valid for this order",
            onUIEvent = {},
        )
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
    WooPosTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PreparingReader(
                title = "Getting ready",
                subtitle = "Preparing reader for payment",
            )
        }
    }
}
