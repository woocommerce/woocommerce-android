package com.woocommerce.android.wear.extensions

import java.text.DecimalFormat

infix fun Number.convertedFrom(denominator: Number): String =
    run { denominator.toDouble() }
        .let { (if (it > 0) (this.toDouble() / it) * PERCENTAGE_BASE else 0.0) }
        .coerceAtMost(PERCENTAGE_BASE)
        .let { DecimalFormat("##.#").format(it) + "%" }

const val PERCENTAGE_BASE = 100.0
