package com.woocommerce.android.ui.woopos.home.phone

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
    val visible = state !is WooPosPhonePersistentButtonState.Hidden
    val lastRenderable = remember { mutableStateOf<WooPosPhonePersistentButtonState?>(null) }
    if (visible) {
        lastRenderable.value = state
    }

    // Don't reserve space before the button has ever appeared in the session.
    // After the first non-Hidden state, the Surface stays laid out so the screen
    // area above it never reflows when the button hides.
    val shown = lastRenderable.value ?: return

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = if (visible) 300 else 100),
        label = "persistent_button_alpha",
    )
    val translateFraction by animateFloatAsState(
        targetValue = if (visible) 0f else 1f,
        animationSpec = tween(durationMillis = if (visible) 300 else 100),
        label = "persistent_button_translate",
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceBright,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                translationY = translateFraction * size.height
            },
    ) {
        // Primary <-> Outlined transitions always go through Hidden (loading totals),
        // so we never need to crossfade between styles while visible. Label changes
        // within the same style snap — standard Android button behavior.
        when (shown) {
            is WooPosPhonePersistentButtonState.Primary -> WooPosButton(
                text = shown.label,
                state = shown.buttonState,
                onClick = { onAction(shown.action) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WooPosSpacing.Medium.value)
                    .navigationBarsPadding(),
            )
            is WooPosPhonePersistentButtonState.Outlined -> WooPosOutlinedButton(
                text = shown.label,
                onClick = { onAction(shown.action) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WooPosSpacing.Medium.value)
                    .navigationBarsPadding(),
            )
            WooPosPhonePersistentButtonState.Hidden -> Unit
        }
    }
}
