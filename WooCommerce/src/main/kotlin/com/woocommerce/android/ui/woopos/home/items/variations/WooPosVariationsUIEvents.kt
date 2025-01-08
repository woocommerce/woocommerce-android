package com.woocommerce.android.ui.woopos.home.items.variations

sealed class WooPosVariationsUIEvents {
    data class EndOfItemsListReached(val productId: Long, val numOfVariations: Int) : WooPosVariationsUIEvents()
    data class PullToRefreshTriggered(val productId: Long) : WooPosVariationsUIEvents()
    data class VariationsLoadingErrorRetryButtonClicked(val productId: Long) : WooPosVariationsUIEvents()
    data class OnItemClicked(val productId: Long, val variationId: Long) : WooPosVariationsUIEvents()
}
