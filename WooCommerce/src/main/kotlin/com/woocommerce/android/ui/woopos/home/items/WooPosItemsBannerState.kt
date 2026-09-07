package com.woocommerce.android.ui.woopos.home.items

sealed class WooPosItemsBannerState {
    data object Hidden : WooPosItemsBannerState()
    data object SyncOverdue : WooPosItemsBannerState()
}
