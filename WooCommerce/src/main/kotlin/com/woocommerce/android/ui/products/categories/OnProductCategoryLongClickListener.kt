package com.woocommerce.android.ui.products.categories

interface OnProductCategoryLongClickListener {
    fun onProductCategoryEditClick(productCategoryItemUiModel: ProductCategoryItemUiModel)
    fun onProductCategoryDeleteClick(productCategoryItemUiModel: ProductCategoryItemUiModel)
}
