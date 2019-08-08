package com.woocommerce.android.extensions

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Date.formatToYYYYmm() = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(this)

fun Date.formatToYYYY() = SimpleDateFormat("yyyy", Locale.getDefault()).format(this)

fun Date.formatToYYYYWmm() = SimpleDateFormat("yyyy-'W'ww", Locale.getDefault()).format(this)
