package com.woocommerce.android.ui.products.selector

sealed class SelectionState {
    data object SELECTED : SelectionState()
    data object UNSELECTED : SelectionState()
    data object PARTIALLY_SELECTED : SelectionState()
    data class DISABLED(val reason: String) : SelectionState()
}
