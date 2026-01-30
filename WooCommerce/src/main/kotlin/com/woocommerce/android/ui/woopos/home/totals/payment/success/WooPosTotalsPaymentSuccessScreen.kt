@file:Suppress("DestructuringDeclarationWithTooManyEntries")

package com.woocommerce.android.ui.woopos.home.totals.payment.success

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSuccessCheckmark
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSuccessCheckmarkAnimationStage
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState
import com.woocommerce.android.ui.woopos.util.WooPosTestTags

@Composable
fun WooPosPaymentSuccessScreen(
    state: WooPosTotalsViewState.PaymentSuccess,
    onReceiptClicked: () -> Unit,
    onNewTransactionClicked: () -> Unit,
) {
    val animationStage = remember { mutableStateOf(WooPosSuccessCheckmarkAnimationStage.INITIAL) }
    val hugeSpacing = WooPosSpacing.Huge.value
    val mediumSpacing = WooPosSpacing.Medium.value
    val xxxLargeSpacing = WooPosSpacing.XXXLarge.value
    val smallSpacing = WooPosSpacing.Small.value
    val marginBetweenButtonAndText by animateDpAsState(
        targetValue = if (animationStage.value >= WooPosSuccessCheckmarkAnimationStage.BUTTONS) {
            hugeSpacing
        } else {
            mediumSpacing
        },
        label = "Check mark size"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceBright)
    ) {
        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (checkmark, title, message, newOrderButton, receiptButton) = createRefs()

            WooPosSuccessCheckmark(
                contentDescription = stringResource(R.string.woopos_payment_successful_label),
                onAnimationStageChanged = { stage -> animationStage.value = stage },
                modifier = Modifier
                    .constrainAs(checkmark) {
                        top.linkTo(parent.top, margin = xxxLargeSpacing)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
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
                    top.linkTo(checkmark.bottom, margin = xxxLargeSpacing)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
            )

            WooPosText(
                text = state.orderTotalText,
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.constrainAs(message) {
                    top.linkTo(title.bottom, margin = smallSpacing)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
            )

            WooPosButton(
                modifier = Modifier
                    .constrainAs(newOrderButton) {
                        bottom.linkTo(receiptButton.top, margin = marginBetweenButtonAndText)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .height(80.dp)
                    .width(604.dp)
                    .testTag(WooPosTestTags.NEW_ORDER_BUTTON),
                onClick = onNewTransactionClicked,
                text = stringResource(R.string.woopos_new_order_button)
            )

            WooPosOutlinedButton(
                modifier = Modifier
                    .constrainAs(receiptButton) {
                        bottom.linkTo(parent.bottom, margin = xxxLargeSpacing)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .height(80.dp)
                    .width(604.dp),
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
            onNewTransactionClicked = {}
        )
    }
}
