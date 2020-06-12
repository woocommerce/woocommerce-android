package com.woocommerce.android.ui.widgets

import androidx.annotation.StringRes
import com.woocommerce.android.R

enum class WidgetColorMode(@StringRes val label: Int) {
    LIGHT(R.string.settings_app_theme_option_light),
    DARK(R.string.settings_app_theme_option_dark)
}
