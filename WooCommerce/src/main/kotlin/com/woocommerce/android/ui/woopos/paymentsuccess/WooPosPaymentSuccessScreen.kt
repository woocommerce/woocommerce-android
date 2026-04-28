package com.woocommerce.android.ui.woopos.paymentsuccess

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSuccessCheckmark
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSuccessCheckmarkAnimationStage
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.adaptiveContentWidth
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

@Composable
fun WooPosPaymentSuccessScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    viewModel: WooPosPaymentSuccessViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            onNavigationEvent(event)
        }
    }

    BackHandler(enabled = true) {
        viewModel.onBackPressed()
    }

    PaymentSuccessContent(
        orderTotalText = state.orderTotalText,
        receiptSentMessage = state.receiptSentMessage,
        onDoneClicked = viewModel::onDoneClicked,
        onEmailReceiptClicked = viewModel::onEmailReceiptClicked,
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
            .background(MaterialTheme.colorScheme.surfaceBright)
            .padding(horizontal = WooPosSpacing.XLarge.value),
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
                    .height(WooPosComponentSize.Small.value)
                    .adaptiveContentWidth(),
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
                    .height(WooPosComponentSize.Small.value)
                    .adaptiveContentWidth(),
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
