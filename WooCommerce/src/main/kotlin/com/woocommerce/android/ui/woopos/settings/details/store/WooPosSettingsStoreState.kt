package com.woocommerce.android.ui.woopos.settings.details.store

sealed class WooPosSettingsStoreState {
    data object Loading : WooPosSettingsStoreState()
    data class Loaded(
        val storeInfo: StoreInfo,
        val receiptInfo: ReceiptInfo? = null
    ) : WooPosSettingsStoreState()

    data class StoreInfo(
        val storeName: String,
        val address: String,
        val phone: String,
        val email: String
    )

    data class ReceiptInfo(
        val storeName: String,
        val address: String,
        val phone: String,
        val email: String,
        val refundPolicy: String
    )
}
