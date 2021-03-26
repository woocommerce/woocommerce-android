package com.woocommerce.android.extensions

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.Product.Image

fun <T> List<T>.areSameAs(otherList: List<T>, isSameAs: T.(T) -> Boolean): Boolean {
    return this.size == otherList.size &&
        this.foldIndexed(true) { i, all, item -> all && item.isSameAs(otherList[i]) }
}

fun List<Image>.areSameImagesAs(images: List<Image>) = this.areSameAs(images) { id == it.id }

fun List<Product>.areSameProductsAs(products: List<Product>) = this.areSameAs(products) { isSameProduct(it) }

fun List<String>.joinToString(separator: String = ", ", lastSeparator: String): String {
    return if (this.size < 3) {
        joinToString(lastSeparator)
    } else {
        (listOf(take(size - 1).joinToString(separator)) + last()).joinToString(lastSeparator)
    }
}

inline fun <T, R, V> List<Pair<R, V>>.pairMap(transform: (R, V) -> T): List<T> =
    map { transform(it.first, it.second) }
