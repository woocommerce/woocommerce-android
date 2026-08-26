package com.woocommerce.android.ui.products.list

import com.woocommerce.android.viewmodel.MultiLiveEvent

sealed class ProductListEvent : MultiLiveEvent.Event() {
    data object ScrollToTop : ProductListEvent()
    data object ShowAddProductBottomSheet : ProductListEvent()
    data object ShowProductSortingBottomSheet : ProductListEvent()
    data class ShowProductFilterScreen(
        val stockStatusFilter: String?,
        val productTypeFilter: String?,
        val productStatusFilter: String?,
        val productCategoryFilter: String?,
        val selectedCategoryName: String?
    ) : ProductListEvent()

    data class ShowProductUpdateStockStatusScreen(val productIds: List<Long>) : ProductListEvent()
    sealed class ShowUpdateDialog : ProductListEvent() {
        abstract val productIds: List<Long>

        data class Price(override val productIds: List<Long>) : ShowUpdateDialog()
        data class Status(override val productIds: List<Long>) : ShowUpdateDialog()
    }

    data class ShowDiscardProductChangesConfirmationDialog(
        val productId: Long,
        val productName: String,
    ) : ProductListEvent()

    data class OpenProduct(val productId: Long) : ProductListEvent()

    data object OpenEmptyProduct : ProductListEvent()

    data object ShowBarcodeScanner : ProductListEvent()
}
