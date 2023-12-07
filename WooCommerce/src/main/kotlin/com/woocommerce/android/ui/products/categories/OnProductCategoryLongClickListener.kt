package com.woocommerce.android.ui.products.categories

import com.woocommerce.android.model.ProductCategory

interface OnProductCategoryLongClickListener {
    fun onProductCategoryEditClick(productCategoryItemUiModel: ProductCategoryItemUiModel)
    fun onProductCategoryDeleteClick(productCategory: ProductCategory)
}
