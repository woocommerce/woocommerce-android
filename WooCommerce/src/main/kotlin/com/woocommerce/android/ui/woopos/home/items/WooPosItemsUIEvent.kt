package com.woocommerce.android.ui.woopos.home.items

sealed class WooPosItemsUIEvent {
    data class OnTabClicked(val tab: WooPosItemsViewState.Tab) : WooPosItemsUIEvent()
    data object BackButtonClicked : WooPosItemsUIEvent()

    data object ClearSearchClicked : WooPosItemsUIEvent()
    data class SearchChanged(
        val query: String,
        val cursorPosition: Int,
    ) : WooPosItemsUIEvent()
    data object CloseSearchClicked : WooPosItemsUIEvent()
    data object SearchAnimationComplete : WooPosItemsUIEvent()
    object SearchIconClicked : WooPosItemsUIEvent()
}
