package com.woocommerce.android.util

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate

/**
 * Helper class for working with Android Dark and Light Themes
 */
object AppThemeUtils {
    @SuppressLint("WrongConstant")
    fun setAppTheme(newTheme: ThemeOption? = null) {
//        val theme = newTheme?.let {
//            AppPrefs.setAppTheme(it)
//            it
//        } ?: AppPrefs.getAppTheme()
        val theme = newTheme ?: ThemeOption.DEFAULT

        when (theme) {
            ThemeOption.LIGHT ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            ThemeOption.DARK ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            ThemeOption.DEFAULT -> {
                if (SystemVersionUtils.isAtLeastQ()) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                }
            }
        }
    }
}

enum class ThemeOption(@StringRes val label: Int) {
    LIGHT(0),
    DARK(1),
    DEFAULT(2)
}
