package com.woocommerce.android.ui.products

import com.woocommerce.android.ui.products.ProductDetailTypesBottomSheetViewModel.ProductTypesBottomSheetUiItem

interface ProductTypeBottomSheetBuilder {
    fun buildBottomSheetList(): List<ProductTypesBottomSheetUiItem>
}
