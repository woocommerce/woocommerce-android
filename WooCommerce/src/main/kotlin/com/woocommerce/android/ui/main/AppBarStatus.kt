package com.woocommerce.android.ui.main

import androidx.annotation.DrawableRes
import com.woocommerce.android.R

sealed class AppBarStatus {
    object Hidden : AppBarStatus()
    data class Visible(
        @DrawableRes
        val navigationIcon: Int? = R.drawable.ic_back_24dp,
        val hasShadow: Boolean = true,
        val hasDivider: Boolean = false
    ) : AppBarStatus()
}
