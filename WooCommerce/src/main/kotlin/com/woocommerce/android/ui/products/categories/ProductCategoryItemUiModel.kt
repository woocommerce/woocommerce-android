package com.woocommerce.android.ui.products.categories

import com.woocommerce.android.model.ProductCategory

data class ProductCategoryItemUiModel(
    val category: ProductCategory,
    var margin: Int = ProductCategory.DEFAULT_PRODUCT_CATEGORY_MARGIN,
    var isSelected: Boolean = false
)
