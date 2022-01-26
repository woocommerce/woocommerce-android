package com.woocommerce.android.util

import kotlin.math.*

/**
 * Returns a rounded value that has the next higher multitude of the same power of 10.
 * Examples when positive number: 62 --> 70, 134 --> 200, 1450 --> 2000
 * Examples when negative number: -62 --> -70, -579 --> -600
 */
fun Float.roundToTheNextPowerOfTen(): Float {
    if (this == 0f) {
        return 0f
    }
    val isNegative = this < 0
    val absoluteValue = abs(this)
    val numberOfDigits = max(floor(log10(absoluteValue)), 0f)
    val tenthPowerValue = 10f.pow(numberOfDigits)
    return if (isNegative) {
        floor(-absoluteValue / tenthPowerValue) * tenthPowerValue
    } else {
        ceil(absoluteValue / tenthPowerValue) * tenthPowerValue
    }
}
