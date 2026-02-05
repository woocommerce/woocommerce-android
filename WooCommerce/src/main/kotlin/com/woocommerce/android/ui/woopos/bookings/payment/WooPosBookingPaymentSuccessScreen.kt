@file:Suppress("DestructuringDeclarationWithTooManyEntries")

package com.woocommerce.android.ui.woopos.bookings.payment

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
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

@Composable
fun WooPosBookingPaymentSuccessScreen(
    orderId: Long,
    amountLabel: String,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
) {
    BackHandler {
        onNavigationEvent(
            WooPosNavigationEvent.GoBackWithResult(BOOKING_PAYMENT_RESULT_KEY, true)
        )
    }

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
            val (icon, title, message, buttonBackToBookings, buttonEmailReceipt) = createRefs()

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
                text = amountLabel,
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.constrainAs(message) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(buttonBackToBookings.top, margin = marginBetweenButtonAndText)
                }
            )

            val marginBetweenButtons = WooPosSpacing.Medium.value
            WooPosButton(
                modifier = Modifier
                    .constrainAs(buttonBackToBookings) {
                        bottom.linkTo(buttonEmailReceipt.top, margin = marginBetweenButtons)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .height(80.dp)
                    .width(604.dp),
                onClick = {
                    onNavigationEvent(
                        WooPosNavigationEvent.GoBackWithResult(BOOKING_PAYMENT_RESULT_KEY, true)
                    )
                },
                text = stringResource(R.string.woopos_bookings_back_to_bookings)
            )

            WooPosOutlinedButton(
                modifier = Modifier
                    .constrainAs(buttonEmailReceipt) {
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .height(80.dp)
                    .width(604.dp),
                onClick = { onNavigationEvent(WooPosNavigationEvent.OpenEmailReceipt(orderId)) },
                text = stringResource(R.string.woopos_receipt_button)
            )
        }
    }
}

@WooPosPreview
@Composable
fun WooPosBookingPaymentSuccessScreenPreview() {
    WooPosTheme {
        WooPosBookingPaymentSuccessScreen(
            orderId = 1L,
            amountLabel = "$13.18",
            onNavigationEvent = {},
        )
    }
}
