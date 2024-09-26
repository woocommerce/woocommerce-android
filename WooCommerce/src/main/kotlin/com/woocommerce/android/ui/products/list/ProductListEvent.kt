package com.woocommerce.android.ui.products.list

import android.view.View
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
    data class ShowProductUpdateStockStatusScreen(val productsIds: List<Long>) : ProductListEvent()
    sealed class ShowUpdateDialog : ProductListEvent() {
        abstract val productsIds: List<Long>

        data class Price(override val productsIds: List<Long>) : ShowUpdateDialog()
        data class Status(override val productsIds: List<Long>) : ShowUpdateDialog()
    }
    data class ShowDiscardProductChangesConfirmationDialog(
        val productId: Long,
        val productName: String,
    ) : ProductListEvent()
    data class OpenProduct(
        val productId: Long,
        val oldPosition: Int,
        val newPosition: Int,
        val sharedView: View?
    ) : ProductListEvent()

    data object OpenEmptyProduct : ProductListEvent()

    data class SelectProducts(val productsIds: List<Long>) : ProductListEvent()
}
