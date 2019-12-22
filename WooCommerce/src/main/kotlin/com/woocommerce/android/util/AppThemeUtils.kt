package com.woocommerce.android.util

import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R

/**
 * Helper class for working with Android Dark and Light Themes
 */
object AppThemeUtils {
    fun setAppTheme(context: Context, newTheme: ThemeOption?) {
        val theme = newTheme?.let {
            AppPrefs.setAppTheme(it)
            it
        } ?: AppPrefs.getAppTheme()

        when (theme) {
            ThemeOption.LIGHT ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            ThemeOption.DARK ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            ThemeOption.DEFAULT -> {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                }
            }
        }
    }
}

enum class ThemeOption(@StringRes val label: Int) {
    LIGHT(R.string.settings_app_theme_option_light),
    DARK(R.string.settings_app_theme_option_dark),
    DEFAULT(R.string.settings_app_theme_option_default)
}
