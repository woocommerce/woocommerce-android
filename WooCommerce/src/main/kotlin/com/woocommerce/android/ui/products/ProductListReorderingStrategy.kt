package com.woocommerce.android.ui.products

import com.woocommerce.android.model.Product

interface ProductListReorderingStrategy {
    fun reOrderItems(from: Int, to: Int, productList: ArrayList<Product>)
}
