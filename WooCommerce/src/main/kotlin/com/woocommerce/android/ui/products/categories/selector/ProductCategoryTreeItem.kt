package com.woocommerce.android.ui.products.categories.selector

import com.woocommerce.android.model.ProductCategory

data class ProductCategoryTreeItem(
    val productCategory: ProductCategory,
    val children: List<ProductCategoryTreeItem>
)

fun List<ProductCategory>.convertToTree(parentId: Long = 0): List<ProductCategoryTreeItem> {
    return filter { it.parentId == parentId }
        .map {
            ProductCategoryTreeItem(
                productCategory = it,
                children = convertToTree(it.remoteCategoryId)
            )
        }
}

fun List<ProductCategory>.convertToFlatTree(): List<ProductCategoryTreeItem> {
    return map {
        ProductCategoryTreeItem(
            productCategory = it,
            children = emptyList()
        )
    }
}
