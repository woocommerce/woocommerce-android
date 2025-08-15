package com.woocommerce.android.ui.woopos.settings.details.store

sealed class WooPosSettingsStoreState {
    data object Loading : WooPosSettingsStoreState()
    data class Loaded(
        val storeInfo: StoreInfo,
        val receiptState: ReceiptState = ReceiptState.NotSupported
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

    sealed class ReceiptState {
        data object NotSupported : ReceiptState()
        data object Loading : ReceiptState()
        data class Success(val receiptInfo: ReceiptInfo) : ReceiptState()
        data object Error : ReceiptState()
    }
}
