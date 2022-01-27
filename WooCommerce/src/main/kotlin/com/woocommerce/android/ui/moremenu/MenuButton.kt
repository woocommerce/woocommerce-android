package com.woocommerce.android.ui.moremenu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class MenuButton(val type: MenuButtonType, @StringRes val text: Int, @DrawableRes val icon: Int)

enum class MenuButtonType {
    VIEW_ADMIN,
    VIEW_STORE,
    REVIEWS
}
