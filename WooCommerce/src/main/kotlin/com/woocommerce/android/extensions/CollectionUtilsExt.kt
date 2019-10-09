package com.woocommerce.android.extensions

/**
 * Extension function that applies a transformation method on each of the list members, then
 * returns the list for chaining.
 *
 * Example:
 * <pre><code>someList.applyTransform { num -> num * 2 }</code></pre>
 *
 * Based on: https://stackoverflow.com/questions/50465292/how-to-apply-map-function-to-array-in-kotlin-and-change-its-values/50466530#50466530
 */
fun <T> List<T>.applyTransform(transform: (T) -> T): List<T> {
    for (i in this.indices) {
        transform(this[i])
    }
    return this
}
