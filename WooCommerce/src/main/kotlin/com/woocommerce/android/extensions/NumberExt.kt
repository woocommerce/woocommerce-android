package com.woocommerce.android.extensions

import kotlin.math.roundToInt

fun Float.formatToString(): String {
    val int = this.roundToInt()
    return if (this != int.toFloat()) {
        this.toString()
    } else {
        int.toString()
    }
}

fun Double?.isInteger() = this?.rem(1) == 0.0

infix fun <T> Comparable<T>?.greaterThan(other: T) =
    this?.let { it > other }
        ?: false
