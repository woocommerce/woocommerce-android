package com.woocommerce.android.ui.woopos.home.totals.payment.success

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Composable
fun WooPosPaymentSuccessScreen(
    state: WooPosTotalsViewState.PaymentSuccess,
    onReceiptClicked: () -> Unit,
    onNewTransactionClicked: () -> Unit,
) {
    val savedAnimationStage = rememberSaveable { mutableStateOf(AnimationStage.INITIAL) }
    val animationStateFlow = remember { MutableStateFlow(savedAnimationStage.value) }

    LaunchedEffect(Unit) {
        if (animationStateFlow.value != AnimationStage.FINISHED) {
            startAnimations(animationStateFlow)
        }
    }

    val animationState = animationStateFlow.collectAsState().value
    savedAnimationStage.value = animationState

    WooPosPaymentSuccessScreen(
        state = state,
        animationStage = animationState,
        onReceiptClicked = onReceiptClicked,
        onNewTransactionClicked = onNewTransactionClicked,
    )
}

@Composable
private fun WooPosPaymentSuccessScreen(
    state: WooPosTotalsViewState.PaymentSuccess,
    animationStage: AnimationStage,
    onReceiptClicked: () -> Unit,
    onNewTransactionClicked: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceBright),
        contentAlignment = Alignment.Center
    ) {
        val marginBetweenButtonAndText by animateDpAsState(
            targetValue = if (animationStage >= AnimationStage.BUTTONS) 80.dp else WooPosSpacing.Medium.value,
            label = "Check mark size"
        )
        @Suppress("DestructuringDeclarationWithTooManyEntries")
        ConstraintLayout {
            val (icon, title, message, buttonNewOrder, buttonEmailReceipts) = createRefs()

            val checkMarkIconMargin = 56.dp.toAdaptivePadding()
            CheckMarkIcon(
                animationStage = animationStage,
                modifier = Modifier.constrainAs(icon) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(title.top, margin = checkMarkIconMargin)
                }
            )

            val textsMargin = WooPosSpacing.Small.value.toAdaptivePadding()
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

            val marginBetweenButtonAndTextAdaptive = marginBetweenButtonAndText.toAdaptivePadding()
            WooPosText(
                text = state.orderTotalText,
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.constrainAs(message) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(buttonNewOrder.top, margin = marginBetweenButtonAndTextAdaptive)
                }
            )

            val marginBetweenButtons = WooPosSpacing.Medium.value.toAdaptivePadding()
            WooPosButton(
                modifier = Modifier
                    .constrainAs(buttonNewOrder) {
                        bottom.linkTo(buttonEmailReceipts.top, margin = marginBetweenButtons)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .height(80.dp)
                    .width(604.dp),
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
                    .height(80.dp)
                    .width(604.dp),
                onClick = onReceiptClicked,
                text = stringResource(R.string.woopos_receipt_button)
            )
        }
    }
}

@Composable
private fun CheckMarkIcon(
    animationStage: AnimationStage,
    modifier: Modifier = Modifier,
) {
    val size by animateDpAsState(
        targetValue = if (animationStage >= AnimationStage.CIRCLE) 166.dp else 0.dp,
        label = "Circle Size"
    )
    val iconSize by animateDpAsState(
        targetValue = if (animationStage >= AnimationStage.ICON) 72.dp else 0.dp,
        label = "Icon Size"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .shadow(
                elevation = WooPosElevation.Medium.value,
                shape = CircleShape,
                clip = false
            )
            .background(WooPosTheme.colors.success, CircleShape)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_woo_pos_check),
            tint = WooPosTheme.colors.onSuccess,
            contentDescription = stringResource(id = R.string.woopos_payment_successful_label),
            modifier = Modifier
                .size(iconSize)
        )
    }
}

@Suppress("MagicNumber")
private suspend fun startAnimations(stateFlow: MutableStateFlow<AnimationStage>) {
    stateFlow.update { AnimationStage.BUTTONS }
    delay(300)
    stateFlow.update { AnimationStage.CIRCLE }
    delay(300)
    stateFlow.update { AnimationStage.ICON }
    stateFlow.update { AnimationStage.FINISHED }
}

private enum class AnimationStage {
    INITIAL,
    BUTTONS,
    CIRCLE,
    ICON,
    FINISHED,
}

@WooPosPreview
@Composable
fun WooPosPaymentSuccessScreenPreview() {
    WooPosTheme {
        WooPosPaymentSuccessScreen(
            state = WooPosTotalsViewState.PaymentSuccess(
                orderTotalText = "A payment of 13.18 was successfully made",
            ),
            animationStage = AnimationStage.FINISHED,
            onReceiptClicked = {},
            onNewTransactionClicked = {}
        )
    }
}
