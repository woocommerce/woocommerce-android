package com.woocommerce.android.util

import java.math.BigDecimal

fun min(a: BigDecimal, b: BigDecimal): BigDecimal = if (a < b) a else b

fun max(a: BigDecimal, b: BigDecimal): BigDecimal = if (a > b) a else b
