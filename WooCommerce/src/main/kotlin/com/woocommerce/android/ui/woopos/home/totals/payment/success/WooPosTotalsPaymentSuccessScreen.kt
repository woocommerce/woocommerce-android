package com.woocommerce.android.ui.woopos.home.totals.payment.success

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState
import kotlinx.coroutines.delay

@Composable
fun WooPosPaymentSuccessScreen(
    state: WooPosTotalsViewState.PaymentSuccess,
    onNewTransactionClicked: () -> Unit,
) {
    var bottomAnimationStarted by remember { mutableStateOf(false) }
    var iconAnimationStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        bottomAnimationStarted = true
        delay(300)
        iconAnimationStarted = true
    }

    WooPosPaymentSuccessScreen(
        state = state,
        iconAnimationStarted = iconAnimationStarted,
        bottomAnimationStarted = bottomAnimationStarted,
        onNewTransactionClicked = onNewTransactionClicked,
    )
}

@Composable
fun WooPosPaymentSuccessScreen(
    state: WooPosTotalsViewState.PaymentSuccess,
    iconAnimationStarted: Boolean,
    bottomAnimationStarted: Boolean,
    onNewTransactionClicked: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WooPosTheme.colors.paymentSuccessBackground),
        contentAlignment = Alignment.Center
    ) {
        val marginBetweenButtonAndText by animateDpAsState(
            targetValue = if (bottomAnimationStarted) 80.dp else 16.dp,
            label = "Check mark size"
        )
        @Suppress("DestructuringDeclarationWithTooManyEntries")
        ConstraintLayout {
            val (icon, title, message, button) = createRefs()

            val checkMarkIconMargin = 56.dp.toAdaptivePadding()
            CheckMarkIcon(
                animationStarted = iconAnimationStarted,
                modifier = Modifier.constrainAs(icon) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(title.top, margin = checkMarkIconMargin)
                }
            )

            val textsMargin = 16.dp.toAdaptivePadding()
            Text(
                text = stringResource(R.string.woopos_payment_successful_label),
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.constrainAs(title) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(message.top, margin = textsMargin)
                }
            )

            val marginBetweenButtonAndTextAdaptive = marginBetweenButtonAndText.toAdaptivePadding()
            Text(
                text = stringResource(R.string.woopos_success_screen_total, state.orderTotalText),
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.constrainAs(message) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(button.top, margin = marginBetweenButtonAndTextAdaptive)
                }
            )

            OutlinedButton(
                modifier = Modifier
                    .constrainAs(button) {
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .height(80.dp)
                    .width(604.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = Color.Transparent,
                ),
                onClick = onNewTransactionClicked,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = R.drawable.woo_pos_ic_return_home),
                    tint = MaterialTheme.colors.onSurface,
                    contentDescription = stringResource(id = R.string.woopos_new_order_button)
                )
                Spacer(modifier = Modifier.width(12.dp.toAdaptivePadding()))
                Text(
                    text = stringResource(R.string.woopos_new_order_button),
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.SemiBold,
                    color = WooPosTheme.colors.paymentSuccessText,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CheckMarkIcon(
    animationStarted: Boolean,
    modifier: Modifier = Modifier,
) {
    val size by animateDpAsState(
        targetValue = if (animationStarted) 164.dp else 0.dp,
        label = "Check mark size"
    )

    var iconAnimationStarted by remember { mutableStateOf(false) }
    val iconSize by animateDpAsState(
        targetValue = if (iconAnimationStarted) 72.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "Icon Size"
    )

    LaunchedEffect(animationStarted) {
        if (animationStarted) {
            delay(300)
            iconAnimationStarted = true
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .shadow(8.dp, CircleShape)
            .background(WooPosTheme.colors.paymentSuccessIconBackground, CircleShape)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            tint = WooPosTheme.colors.paymentSuccessIcon,
            contentDescription = stringResource(id = R.string.woopos_payment_successful_label),
            modifier = Modifier
                .size(iconSize)
        )
    }
}

@WooPosPreview
@Composable
fun WooPosPaymentSuccessScreenPreview() {
    WooPosTheme {
        WooPosPaymentSuccessScreen(
            state = WooPosTotalsViewState.PaymentSuccess(orderTotalText = "$13.18"),
            bottomAnimationStarted = true,
            iconAnimationStarted = true,
            onNewTransactionClicked = {}
        )
    }
}
