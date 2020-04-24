package com.woocommerce.android.extensions

import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP

infix fun BigDecimal?.isEqualTo(x: BigDecimal?) = this == x || (x != null && this?.compareTo(x) == 0)

infix fun BigDecimal?.isNotEqualTo(x: BigDecimal?) = !this.isEqualTo(x)

fun BigDecimal.roundError(): BigDecimal = this.setScale(2, HALF_UP)
