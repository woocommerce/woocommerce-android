package com.woocommerce.android.extensions

import java.util.Locale
import javax.inject.Inject

class NumberExtensionsWrapper @Inject constructor() {
    fun compactNumberCompat(
        number: Long,
        locale: Locale = Locale.getDefault()
    ): String = compactNumberCompat(number, locale)
}
