package com.woocommerce.android.ui.woopos.paymentsuccess

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.cardpayment.BOOKING_CARD_PAYMENT_SUCCESS_KEY
import com.woocommerce.android.ui.woopos.cashpayment.BOOKING_CASH_PAYMENT_SUCCESS_KEY
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSuccessCheckmark
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSuccessCheckmarkAnimationStage
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

@Composable
fun WooPosPaymentSuccessScreen(
    orderId: Long,
    orderTotalText: String,
    source: PaymentSuccessSource,
    receiptSentMessage: String?,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    viewModel: WooPosPaymentSuccessViewModel = hiltViewModel(),
) {
    BackHandler(enabled = true) { }

    PaymentSuccessContent(
        orderTotalText = orderTotalText,
        receiptSentMessage = receiptSentMessage,
        onDoneClicked = {
            when (source) {
                PaymentSuccessSource.CARD_CHECKOUT -> {
                    onNavigationEvent(WooPosNavigationEvent.GoBack)
                }
                PaymentSuccessSource.CARD_BOOKINGS -> {
                    onNavigationEvent(
                        WooPosNavigationEvent.NavigateBackToBookingsAfterPayment(
                            BOOKING_CARD_PAYMENT_SUCCESS_KEY,
                            true
                        )
                    )
                }
                PaymentSuccessSource.CASH_BOOKINGS -> {
                    onNavigationEvent(
                        WooPosNavigationEvent.NavigateBackToBookingsAfterPayment(
                            BOOKING_CASH_PAYMENT_SUCCESS_KEY,
                            true
                        )
                    )
                }
            }
        },
        onEmailReceiptClicked = {
            viewModel.onEmailReceiptClicked()
            onNavigationEvent(WooPosNavigationEvent.OpenEmailReceipt(orderId))
        },
    )
}

@Suppress("DestructuringDeclarationWithTooManyEntries")
@Composable
private fun PaymentSuccessContent(
    orderTotalText: String,
    receiptSentMessage: String?,
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
        val receiptSentMargin = WooPosSpacing.Medium.value

        ConstraintLayout {
            val (icon, title, message, buttonDone, buttonEmailReceipts) = createRefs()
            val receiptSent = createRef()

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
                text = orderTotalText,
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.constrainAs(message) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(receiptSent.top, margin = receiptSentMargin)
                }
            )

            WooPosText(
                text = receiptSentMessage.orEmpty(),
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.constrainAs(receiptSent) {
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

@WooPosPreview
@Composable
fun PaymentSuccessScreenPreview() {
    WooPosTheme {
        PaymentSuccessContent(
            orderTotalText = "A card payment of $12.50 was successfully made.",
            receiptSentMessage = "A receipt has been sent to customer@example.com.",
            onDoneClicked = {},
            onEmailReceiptClicked = {},
        )
    }
}
