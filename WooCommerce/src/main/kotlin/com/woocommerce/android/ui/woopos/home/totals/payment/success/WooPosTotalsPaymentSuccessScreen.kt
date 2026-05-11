@file:Suppress("DestructuringDeclarationWithTooManyEntries")

package com.woocommerce.android.ui.woopos.home.totals.payment.success

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.constraintlayout.compose.ConstraintLayout
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
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState
import com.woocommerce.android.ui.woopos.util.WooPosTestTags

@Composable
fun WooPosPaymentSuccessScreen(
    state: WooPosTotalsViewState.PaymentSuccess,
    onReceiptClicked: () -> Unit,
    onNewTransactionClicked: () -> Unit,
    onBackPressed: () -> Unit,
) {
    BackHandler(onBack = onBackPressed)
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
            val (icon, title, message, buttonNewOrder, buttonEmailReceipts) = createRefs()

            WooPosSuccessCheckmark(
                contentDescription = stringResource(R.string.woopos_payment_successful_label),
                onAnimationStageChanged = { stage -> animationStage.value = stage },
                modifier = Modifier
                    .constrainAs(icon) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(title.top, margin = checkMarkIconMargin)
                    }
                    .testTag(WooPosTestTags.SUCCESS_CHECKMARK_ICON)
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
                    bottom.linkTo(buttonNewOrder.top, margin = marginBetweenButtonAndText)
                }
            )

            val marginBetweenButtons = WooPosSpacing.Medium.value
            WooPosButton(
                modifier = Modifier
                    .constrainAs(buttonNewOrder) {
                        bottom.linkTo(buttonEmailReceipts.top, margin = marginBetweenButtons)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .height(WooPosComponentSize.Small.value)
                    .adaptiveContentWidth()
                    .padding(horizontal = WooPosSpacing.XLarge.value)
                    .testTag(WooPosTestTags.NEW_ORDER_BUTTON),
                onClick = onNewTransactionClicked,
                text = stringResource(R.string.woopos_new_order_button)
            )

            WooPosOutlinedButton(
                modifier = Modifier
                    .constrainAs(buttonEmailReceipts) {
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .height(WooPosComponentSize.Small.value)
                    .adaptiveContentWidth()
                    .padding(horizontal = WooPosSpacing.XLarge.value),
                onClick = onReceiptClicked,
                text = stringResource(R.string.woopos_receipt_button)
            )
        }
    }
}

@WooPosPreview
@Composable
fun WooPosPaymentSuccessScreenPreview() {
    WooPosTheme {
        WooPosPaymentSuccessScreen(
            state = WooPosTotalsViewState.PaymentSuccess(
                orderTotalText = "A payment of 13.18 was successfully made",
            ),
            onReceiptClicked = {},
            onNewTransactionClicked = {},
            onBackPressed = {},
        )
    }
}
