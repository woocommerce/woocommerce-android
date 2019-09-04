package com.woocommerce.android.util

import java.math.BigDecimal

infix fun BigDecimal.isEqualTo(x: BigDecimal) = this.compareTo(x) == 0
