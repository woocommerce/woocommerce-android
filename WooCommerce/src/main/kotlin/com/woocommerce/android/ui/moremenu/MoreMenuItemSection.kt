package com.woocommerce.android.ui.moremenu

import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class MoreMenuItemSection(
    @StringRes val title: Int?,
    val items: List<MoreMenuItem>,
    val isVisible: Boolean = true,
)

sealed class MoreMenuItem(open val isVisible: Boolean) {
    data class Button(
        @StringRes val title: Int,
        @StringRes val description: Int,
        @DrawableRes val icon: Int,
        @DrawableRes val extraIcon: Int? = null,
        override val isVisible: Boolean = true,
        val badgeState: BadgeState? = null,
        val onClick: () -> Unit = {},
    ) : MoreMenuItem(isVisible)

    data class Loading(
        override val isVisible: Boolean,
    ) : MoreMenuItem(isVisible)
}

data class BadgeState(
    @DimenRes val badgeSize: Int,
    @ColorRes val backgroundColor: Int,
    @ColorRes val textColor: Int,
    val textState: TextState,
    val animateAppearance: Boolean = false,
)

data class TextState(val text: String, @DimenRes val fontSize: Int)
