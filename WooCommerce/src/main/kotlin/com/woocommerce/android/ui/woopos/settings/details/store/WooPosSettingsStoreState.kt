package com.woocommerce.android.ui.woopos.settings.details.store

data class WooPosSettingsStoreState(
    val storeInfoState: StoreState = StoreState.Loading,
    val receiptState: ReceiptState = ReceiptState.Loading
) {
    data class StoreInfo(
        val storeName: String,
        val address: String
    )

    data class ReceiptInfo(
        val storeName: String,
        val address: String,
        val phone: String,
        val refundPolicy: String
    )

    sealed class ReceiptState {
        data object NotSupported : ReceiptState()
        data object Loading : ReceiptState()
        data class Success(val receiptInfo: ReceiptInfo) : ReceiptState()
        data object Error : ReceiptState()
    }

    sealed class StoreState {
        data object Loading : StoreState()
        data class Loaded(val storeInfo: StoreInfo) : StoreState()
    }
}
