package com.woocommerce.android.extensions

import java.util.Locale
import javax.inject.Inject
import com.woocommerce.android.extensions.compactNumberCompat as compactNumberCompatExt

class NumberExtensionsWrapper @Inject constructor() {
    fun compactNumberCompat(
        number: Long,
        locale: Locale = Locale.getDefault()
    ): String = compactNumberCompatExt(number, locale)
}
