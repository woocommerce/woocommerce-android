package com.woocommerce.android.ui.products

import com.woocommerce.android.model.Product

class DefaultProductListSwappingStrategy: ProductListSwappingStrategy {
    override fun reOrderItems(from: Int, to: Int, productList: ArrayList<Product>) {
        val fromValue = productList[from]
        productList[from] = productList[to]
        productList[to] = fromValue
    }
}
