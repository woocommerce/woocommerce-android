package com.woocommerce.android.extensions

import java.text.DecimalFormat
import kotlin.math.roundToInt

fun Float.formatToString(): String {
    val int = this.roundToInt()
    return if (this != int.toFloat()) {
        this.toString()
    } else {
        int.toString()
    }
}

fun Double.formatToString(): String {
    val int = this.roundToInt()
    return if (this != int.toDouble()) {
        this.toString()
    } else {
        int.toString()
    }
}

fun Double?.isInteger() = this?.rem(1) == 0.0

infix fun Number.convertedFrom(denominator: Number): String =
    run { denominator.toDouble() }
        .let { (if (it > 0) (this.toDouble() / it) * PERCENTAGE_BASE else 0.0) }
        .coerceAtMost(PERCENTAGE_BASE)
        .let { DecimalFormat("##.#").format(it) + "%" }

infix fun <T> Comparable<T>?.greaterThan(other: T) =
    this?.let { it > other }
        ?: false

infix fun <T> Comparable<T>?.lesserThan(other: T) =
    this?.let { it < other }
        ?: false

/**
 * The number is shortened to the nearest thousand or million. For example, 1,500 is shortened to 1.5k.
 */
fun Long.shortenToNearestThousand(): String = when {
    this >= ONE_MILLION -> "${(this / ONE_MILLION)}m"
    this >= ONE_THOUSAND -> "${(this / ONE_THOUSAND)}k"
    else -> this.toString()
}

const val PERCENTAGE_BASE = 100.0
const val ONE_THOUSAND = 1000
const val ONE_MILLION = 1000000
