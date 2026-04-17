package com.woocommerce.android.ui.woopos.home.phone

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.home.WooPosHomeState
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartState
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState

enum class PhonePersistentButtonAction {
    OpenCart,
    Checkout,
    CashPayment,
}

sealed class PhonePersistentButtonState {
    data object Hidden : PhonePersistentButtonState()

    data class Primary(
        val label: String,
        val buttonState: WooPosButtonState,
        val action: PhonePersistentButtonAction,
    ) : PhonePersistentButtonState()

    data class Outlined(
        val label: String,
        val action: PhonePersistentButtonAction,
    ) : PhonePersistentButtonState()
}

@Suppress("LongParameterList")
fun resolvePhonePersistentButtonState(
    screenPositionState: WooPosHomeState.ScreenPositionState,
    cartState: WooPosCartState?,
    totalsState: WooPosTotalsViewState?,
    cartLabelBuilder: (itemCount: Int) -> String,
    checkoutLabel: String,
    cashPaymentLabel: String,
): PhonePersistentButtonState = when (screenPositionState) {
    WooPosHomeState.ScreenPositionState.Products -> {
        val body = cartState?.body
        when (body) {
            is WooPosCartState.Body.WithItems -> {
                if (body.amountOfItems > 0) {
                    PhonePersistentButtonState.Primary(
                        label = cartLabelBuilder(body.amountOfItems),
                        buttonState = WooPosButtonState.ENABLED,
                        action = PhonePersistentButtonAction.OpenCart,
                    )
                } else {
                    PhonePersistentButtonState.Hidden
                }
            }
            WooPosCartState.Body.Empty,
            null -> PhonePersistentButtonState.Hidden
        }
    }
    WooPosHomeState.ScreenPositionState.Cart -> {
        when (cartState?.checkoutButtonState) {
            WooPosCartState.CheckoutButtonState.Enabled -> PhonePersistentButtonState.Primary(
                label = checkoutLabel,
                buttonState = WooPosButtonState.ENABLED,
                action = PhonePersistentButtonAction.Checkout,
            )
            WooPosCartState.CheckoutButtonState.Disabled -> PhonePersistentButtonState.Primary(
                label = checkoutLabel,
                buttonState = WooPosButtonState.DISABLED,
                action = PhonePersistentButtonAction.Checkout,
            )
            WooPosCartState.CheckoutButtonState.Invisible,
            null -> PhonePersistentButtonState.Hidden
        }
    }
    is WooPosHomeState.ScreenPositionState.Checkout -> {
        when (totalsState) {
            is WooPosTotalsViewState.Checkout -> PhonePersistentButtonState.Outlined(
                label = cashPaymentLabel,
                action = PhonePersistentButtonAction.CashPayment,
            )
            WooPosTotalsViewState.Loading,
            is WooPosTotalsViewState.PaymentInProgress,
            is WooPosTotalsViewState.PaymentSuccess,
            is WooPosTotalsViewState.PaymentFailed,
            is WooPosTotalsViewState.Error,
            is WooPosTotalsViewState.InvalidCouponError,
            is WooPosTotalsViewState.ProductNotFoundError,
            null -> PhonePersistentButtonState.Hidden
        }
    }
}

@Composable
fun WooPosHomePhonePersistentButton(
    state: PhonePersistentButtonState,
    onAction: (PhonePersistentButtonAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        PhonePersistentButtonState.Hidden -> Unit

        is PhonePersistentButtonState.Primary -> {
            Surface(
                color = MaterialTheme.colorScheme.surfaceBright,
                modifier = modifier.fillMaxWidth(),
            ) {
                WooPosButton(
                    text = state.label,
                    onClick = { onAction(state.action) },
                    state = state.buttonState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WooPosSpacing.Medium.value)
                        .navigationBarsPadding(),
                )
            }
        }

        is PhonePersistentButtonState.Outlined -> {
            Surface(
                color = MaterialTheme.colorScheme.surfaceBright,
                modifier = modifier.fillMaxWidth(),
            ) {
                WooPosOutlinedButton(
                    text = state.label,
                    onClick = { onAction(state.action) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WooPosSpacing.Medium.value)
                        .navigationBarsPadding(),
                )
            }
        }
    }
}
