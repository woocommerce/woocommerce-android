package com.woocommerce.android.extensions

import android.content.Context
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.MenuItem
import androidx.core.content.ContextCompat

fun MenuItem.colorizeTitle(context: Context?, colorResource: Int) = apply {
    context?.let { ContextCompat.getColor(it, colorResource) }
        ?.let {
            SpannableString(this.title).apply {
                setSpan(ForegroundColorSpan(it), 0, title.length, 0)
            }
        }.let { this.title = it }
}
