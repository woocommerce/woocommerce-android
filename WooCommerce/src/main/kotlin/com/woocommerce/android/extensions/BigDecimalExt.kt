package com.woocommerce.android.extensions

import java.math.BigDecimal

infix fun BigDecimal.isEqualTo(x: BigDecimal) = this.compareTo(x) == 0
infix fun BigDecimal.isNotEqualTo(x: BigDecimal) = this.compareTo(x) != 0
