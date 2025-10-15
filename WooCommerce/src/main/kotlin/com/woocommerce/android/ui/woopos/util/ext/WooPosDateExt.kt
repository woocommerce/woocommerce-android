package com.woocommerce.android.ui.woopos.util.ext

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Date.formatToMMMddYYYYAtHHmm(
    locale: Locale = Locale.getDefault(),
    atWord: String
): String = SimpleDateFormat("MMM d, yyyy '$atWord' h:mm a", locale).format(this)
