package com.woocommerce.android.ui.woopos.home.items

sealed class WooPosItemsUIEvent {
    data class OnTabClicked(val tab: WooPosItemsToolbarViewState.Tab) : WooPosItemsUIEvent()
    data object BackFromVariationsClicked : WooPosItemsUIEvent()

    data object ClearSearchClicked : WooPosItemsUIEvent()
    data class SearchChanged(
        val query: String,
        val cursorPosition: Int,
    ) : WooPosItemsUIEvent()
    data object CloseSearchClicked : WooPosItemsUIEvent()
    data object SearchIconClicked : WooPosItemsUIEvent()
    data object AddCouponIconClicked : WooPosItemsUIEvent()
    data object SyncOverdueBannerDismissed : WooPosItemsUIEvent()
}
