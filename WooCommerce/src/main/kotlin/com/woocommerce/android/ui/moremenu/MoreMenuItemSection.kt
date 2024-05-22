package com.woocommerce.android.ui.moremenu

import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class MoreMenuItemSection(
    @StringRes val title: Int?,
    val items: List<MoreMenuItemButton>,
    val isVisible: Boolean = true,
)

data class MoreMenuItemButton(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val extraIcon: Int? = null,
    val isVisible: Boolean = true,
    val badgeState: BadgeState? = null,
    val withDivider: Boolean = true,
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
