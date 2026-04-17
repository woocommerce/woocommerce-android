package com.woocommerce.android.ui.woopos.home.phone

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.home.WooPosHomeState
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartState
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState

enum class WooPosPhonePersistentButtonAction {
    OpenCart,
    Checkout,
    CashPayment,
}

sealed class WooPosPhonePersistentButtonState {
    data object Hidden : WooPosPhonePersistentButtonState()

    data class Primary(
        val label: String,
        val buttonState: WooPosButtonState,
        val action: WooPosPhonePersistentButtonAction,
    ) : WooPosPhonePersistentButtonState()

    data class Outlined(
        val label: String,
        val action: WooPosPhonePersistentButtonAction,
    ) : WooPosPhonePersistentButtonState()
}

class WooPosPhonePersistentButtonStateResolver(
    private val buildCartLabel: (itemCount: Int) -> String,
    private val checkoutLabel: String,
    private val cashPaymentLabel: String,
) {
    fun resolve(
        screenPositionState: WooPosHomeState.ScreenPositionState,
        cartState: WooPosCartState?,
        totalsState: WooPosTotalsViewState?,
    ): WooPosPhonePersistentButtonState = when (screenPositionState) {
        WooPosHomeState.ScreenPositionState.Products -> {
            val body = cartState?.body
            when (body) {
                is WooPosCartState.Body.WithItems -> {
                    if (body.amountOfItems > 0) {
                        WooPosPhonePersistentButtonState.Primary(
                            label = buildCartLabel(body.amountOfItems),
                            buttonState = WooPosButtonState.ENABLED,
                            action = WooPosPhonePersistentButtonAction.OpenCart,
                        )
                    } else {
                        WooPosPhonePersistentButtonState.Hidden
                    }
                }
                WooPosCartState.Body.Empty,
                null -> WooPosPhonePersistentButtonState.Hidden
            }
        }
        WooPosHomeState.ScreenPositionState.Cart -> {
            when (cartState?.checkoutButtonState) {
                WooPosCartState.CheckoutButtonState.Enabled -> WooPosPhonePersistentButtonState.Primary(
                    label = checkoutLabel,
                    buttonState = WooPosButtonState.ENABLED,
                    action = WooPosPhonePersistentButtonAction.Checkout,
                )
                WooPosCartState.CheckoutButtonState.Disabled -> WooPosPhonePersistentButtonState.Primary(
                    label = checkoutLabel,
                    buttonState = WooPosButtonState.DISABLED,
                    action = WooPosPhonePersistentButtonAction.Checkout,
                )
                WooPosCartState.CheckoutButtonState.Invisible,
                null -> WooPosPhonePersistentButtonState.Hidden
            }
        }
        is WooPosHomeState.ScreenPositionState.Checkout -> {
            when (totalsState) {
                is WooPosTotalsViewState.Checkout -> WooPosPhonePersistentButtonState.Outlined(
                    label = cashPaymentLabel,
                    action = WooPosPhonePersistentButtonAction.CashPayment,
                )
                WooPosTotalsViewState.Loading,
                is WooPosTotalsViewState.PaymentInProgress,
                is WooPosTotalsViewState.PaymentSuccess,
                is WooPosTotalsViewState.PaymentFailed,
                is WooPosTotalsViewState.Error,
                is WooPosTotalsViewState.InvalidCouponError,
                is WooPosTotalsViewState.ProductNotFoundError,
                null -> WooPosPhonePersistentButtonState.Hidden
            }
        }
    }
}

@Composable
fun WooPosHomePhonePersistentButton(
    state: WooPosPhonePersistentButtonState,
    onAction: (WooPosPhonePersistentButtonAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lastRenderable = remember { mutableStateOf<WooPosPhonePersistentButtonState?>(null) }
    if (state !is WooPosPhonePersistentButtonState.Hidden) {
        lastRenderable.value = state
    }

    AnimatedVisibility(
        visible = state !is WooPosPhonePersistentButtonState.Hidden,
        enter = slideInVertically(tween(durationMillis = 300)) { it } + fadeIn(tween(300)),
        exit = slideOutVertically(tween(durationMillis = 300)) { it } + fadeOut(tween(100)),
        modifier = modifier,
    ) {
        val shown: WooPosPhonePersistentButtonState? =
            if (state !is WooPosPhonePersistentButtonState.Hidden) state else lastRenderable.value

        Surface(
            color = MaterialTheme.colorScheme.surfaceBright,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AnimatedContent(
                targetState = shown,
                contentKey = { s ->
                    when (s) {
                        is WooPosPhonePersistentButtonState.Primary -> "P:${s.label}"
                        is WooPosPhonePersistentButtonState.Outlined -> "O:${s.label}"
                        WooPosPhonePersistentButtonState.Hidden, null -> "H"
                    }
                },
                transitionSpec = {
                    (fadeIn(tween(200)) togetherWith fadeOut(tween(200)))
                        .using(SizeTransform(clip = false))
                },
                label = "persistent_button_morph",
            ) { s ->
                when (s) {
                    is WooPosPhonePersistentButtonState.Primary -> WooPosButton(
                        text = s.label,
                        state = s.buttonState,
                        onClick = { onAction(s.action) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(WooPosSpacing.Medium.value)
                            .navigationBarsPadding(),
                    )
                    is WooPosPhonePersistentButtonState.Outlined -> WooPosOutlinedButton(
                        text = s.label,
                        onClick = { onAction(s.action) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(WooPosSpacing.Medium.value)
                            .navigationBarsPadding(),
                    )
                    WooPosPhonePersistentButtonState.Hidden, null -> Unit
                }
            }
        }
    }
}
