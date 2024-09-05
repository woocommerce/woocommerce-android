package com.woocommerce.android.extensions

import android.icu.number.Notation
import android.icu.number.NumberFormatter
import android.icu.text.CompactDecimalFormat
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import java.text.DecimalFormat
import java.util.Locale
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
 * Shortens a number to the nearest thousand and appends a 'K' to the end. For example, 1000 will be shortened to 1K.
 */
fun compactNumberCompat(number: Long, locale: Locale = Locale.getDefault()): String =
    if (VERSION.SDK_INT >= VERSION_CODES.R) {
        NumberFormatter.with()
            .notation(Notation.compactShort())
            .locale(locale)
            .format(number)
            .toString()
    } else {
        CompactDecimalFormat.getInstance(locale, CompactDecimalFormat.CompactStyle.SHORT)
            .format(number.toDouble())
    }

const val PERCENTAGE_BASE = 100.0
