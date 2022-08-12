package com.woocommerce.android.ui.moremenu

import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class MenuUiButton(
    @StringRes val text: Int,
    @DrawableRes val icon: Int,
    val badgeState: BadgeState? = null,
    val isEnabled: Boolean = true,
    val onClick: () -> Unit = {},
)

data class BadgeState(
    @DimenRes val badgeSize: Int,
    @ColorRes val backgroundColor: Int,
    @ColorRes val textColor: Int,
    val textState: TextState,
    val animateAppearance: Boolean = false,
)

data class TextState(val text: String, @DimenRes val fontSize: Int)
