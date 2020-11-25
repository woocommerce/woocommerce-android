package com.woocommerce.android.util

import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

fun Fragment.setHomeIcon(@DrawableRes icon: Int) {
    (requireActivity() as AppCompatActivity).supportActionBar?.apply {
        setDisplayHomeAsUpEnabled(true)
        setHomeAsUpIndicator(icon)
    }
}
