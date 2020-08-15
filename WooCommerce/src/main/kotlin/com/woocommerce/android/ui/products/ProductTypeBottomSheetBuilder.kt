package com.woocommerce.android.ui.products

import com.woocommerce.android.ui.products.ProductTypesBottomSheetViewModel.ProductTypesBottomSheetUiItem

interface ProductTypeBottomSheetBuilder {
    fun buildBottomSheetList(): List<ProductTypesBottomSheetUiItem>
}
