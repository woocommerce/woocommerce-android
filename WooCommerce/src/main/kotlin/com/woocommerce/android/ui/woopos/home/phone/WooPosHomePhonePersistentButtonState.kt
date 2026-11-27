package com.woocommerce.android.ui.woopos.home.phone

import com.woocommerce.android.ui.woopos.home.cart.WooPosCartState

internal sealed interface WooPosHomePhonePersistentButtonState {
    data object Hidden : WooPosHomePhonePersistentButtonState

    data class OpenCart(val itemCount: Int) : WooPosHomePhonePersistentButtonState

    data class Checkout(val isEnabled: Boolean) : WooPosHomePhonePersistentButtonState
}

internal fun phonePersistentButtonState(
    cartState: WooPosCartState?,
    isSheetOpen: Boolean,
): WooPosHomePhonePersistentButtonState {
    val itemCount = cartState?.body?.amountOfItems ?: 0
    return when {
        itemCount == 0 -> WooPosHomePhonePersistentButtonState.Hidden
        isSheetOpen -> WooPosHomePhonePersistentButtonState.Checkout(
            isEnabled = cartState?.checkoutButtonState != WooPosCartState.CheckoutButtonState.Disabled
        )
        else -> WooPosHomePhonePersistentButtonState.OpenCart(itemCount)
    }
}
