package com.woocommerce.android.ui.woopos.home.phone

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.home.WooPosHomeState
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartState
import com.woocommerce.android.ui.woopos.home.cart.toWooPosButtonState

enum class WooPosPhonePersistentButtonAction {
    OpenCart,
    Checkout,
}

sealed class WooPosPhonePersistentButtonState {
    data object Hidden : WooPosPhonePersistentButtonState()

    data class Primary(
        val label: String,
        val buttonState: WooPosButtonState,
        val action: WooPosPhonePersistentButtonAction,
    ) : WooPosPhonePersistentButtonState()
}

class WooPosPhonePersistentButtonStateResolver(
    private val buildCartLabel: (itemCount: Int) -> String,
    private val checkoutLabel: String,
) {
    fun resolve(
        screenPositionState: WooPosHomeState.ScreenPositionState,
        cartState: WooPosCartState?,
    ): WooPosPhonePersistentButtonState = when (screenPositionState) {
        WooPosHomeState.ScreenPositionState.Products -> {
            val body = cartState?.body
            when (body) {
                is WooPosCartState.Body.WithItems -> {
                    if (body.amountOfItems > 0) {
                        WooPosPhonePersistentButtonState.Primary(
                            label = buildCartLabel(body.amountOfItems),
                            buttonState = cartState.checkoutButtonState.toWooPosButtonState()
                                ?: WooPosButtonState.ENABLED,
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
            cartState?.checkoutButtonState?.toWooPosButtonState()?.let { buttonState ->
                WooPosPhonePersistentButtonState.Primary(
                    label = checkoutLabel,
                    buttonState = buttonState,
                    action = WooPosPhonePersistentButtonAction.Checkout,
                )
            } ?: WooPosPhonePersistentButtonState.Hidden
        }
        is WooPosHomeState.ScreenPositionState.Checkout -> WooPosPhonePersistentButtonState.Hidden
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
    val shown = lastRenderable.value

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
        exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(200)),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (shown == null) return@AnimatedVisibility
        Surface(
            color = MaterialTheme.colorScheme.surfaceBright,
            modifier = Modifier.fillMaxWidth(),
        ) {
            when (shown) {
                is WooPosPhonePersistentButtonState.Primary -> WooPosButton(
                    text = shown.label,
                    state = shown.buttonState,
                    onClick = { onAction(shown.action) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WooPosSpacing.Medium.value)
                        .padding(bottom = WooPosSpacing.Small.value)
                        .navigationBarsPadding(),
                )
                WooPosPhonePersistentButtonState.Hidden -> Unit
            }
        }
    }
}
