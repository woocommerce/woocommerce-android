package com.woocommerce.android.ui.woopos.home.cart

import com.woocommerce.android.ui.woopos.common.composeui.modifier.BarcodeInputDetector

sealed class WooPosCartUIEvent {
    data object CheckoutClicked : WooPosCartUIEvent()
    data class ItemRemovedFromCart(
        val item: WooPosCartItemViewState
    ) : WooPosCartUIEvent()
    data object ClearAllClicked : WooPosCartUIEvent()
    data object BackClicked : WooPosCartUIEvent()
    data class OnBarcodeScanned(
        val barcode: String,
        val metadata: BarcodeInputDetector.ScanMetadata
    ) : WooPosCartUIEvent()
}
