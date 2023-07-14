package com.woocommerce.android.ui.products.categories

import com.woocommerce.android.R
import com.woocommerce.android.model.ProductCategory

data class ProductCategoryItemUiModel(
    val category: ProductCategory,
    var margin: Int = R.dimen.major_125,
    var isSelected: Boolean = false
)
