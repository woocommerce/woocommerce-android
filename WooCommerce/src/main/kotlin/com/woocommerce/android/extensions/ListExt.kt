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

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each [Pair.first] and [Pair.second] in the original collection.
 *
 * This method extend a map operation to be able to take a List of Pairs and map both values declared
 * in a explicit parameter form. The objective of this is to improve the readability when composing
 * complex data through the usage of [Pair]. So instead of access the Pair using [Pair.first] and [Pair.second]
 * this method allow us to define better naming for the map operation by wrapping the object in two parameters
 * inside the [transform] HOF declaration of the caller
 */
inline fun <T, R, V> List<Pair<R, V>>.pairMap(transform: (R, V) -> T): List<T> =
    map { transform(it.first, it.second) }
