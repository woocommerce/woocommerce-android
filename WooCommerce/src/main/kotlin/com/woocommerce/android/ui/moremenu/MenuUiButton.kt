package com.woocommerce.android.ui.moremenu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class MenuUiButton(
    val type: MenuButtonType,
    @StringRes val text: Int,
    @DrawableRes val icon: Int,
    val badgeCount: Int = 0,
    val isEnabled: Boolean = true,
    val onClick: () -> Unit = {},
)

enum class MenuButtonType {
    VIEW_ADMIN,
    VIEW_STORE,
    COUPONS,
    PRODUCT_REVIEWS,
    INBOX
}
