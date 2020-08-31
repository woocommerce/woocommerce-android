package com.woocommerce.android.extensions

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.Product.Image

fun <T> List<T>.areSameAs(otherList: List<T>, isSameAs: T.(T) -> Boolean): Boolean {
    return this.size == otherList.size &&
        this.foldIndexed(true) { i, all, item -> all && item.isSameAs(otherList[i]) }
}

fun List<Image>.areSameImagesAs(images: List<Image>) = this.areSameAs(images) { id == it.id }

fun List<Product>.areSameProductsAs(products: List<Product>) = this.areSameAs(products) { isSameProduct(it) }
