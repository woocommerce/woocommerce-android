package com.woocommerce.android.util

import android.view.View

fun Boolean.toVisibility(): Int = if (this) View.VISIBLE else View.GONE
