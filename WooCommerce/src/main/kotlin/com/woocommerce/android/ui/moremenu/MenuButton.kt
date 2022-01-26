package com.woocommerce.android.ui.moremenu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class MenuButton(
    @StringRes val text: Int,
    @DrawableRes val icon: Int,
    val badgeCount: Int = 0,
    val onClick: () -> Unit = {},
)
