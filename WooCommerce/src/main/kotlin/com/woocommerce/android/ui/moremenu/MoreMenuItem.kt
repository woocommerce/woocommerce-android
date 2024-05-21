package com.woocommerce.android.ui.moremenu

import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

sealed class MoreMenuItem {
    abstract val isEnabled: Boolean

    data class Button(
        @StringRes val title: Int,
        @StringRes val description: Int,
        @DrawableRes val icon: Int,
        override val isEnabled: Boolean = true,
        val badgeState: BadgeState? = null,
        val onClick: () -> Unit = {},
    ) : MoreMenuItem()

    data class Header(
        @StringRes val title: Int,
        override val isEnabled: Boolean = true,
    ) : MoreMenuItem()
}


data class BadgeState(
    @DimenRes val badgeSize: Int,
    @ColorRes val backgroundColor: Int,
    @ColorRes val textColor: Int,
    val textState: TextState,
    val animateAppearance: Boolean = false,
)

data class TextState(val text: String, @DimenRes val fontSize: Int)
