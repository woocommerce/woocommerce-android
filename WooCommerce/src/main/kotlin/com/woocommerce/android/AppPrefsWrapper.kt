package com.woocommerce.android

import com.woocommerce.android.ui.widgets.WidgetColorMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injectable wrapper around AppPrefs.
 *
 * AppPrefs interface consists of static methods, which make the client code difficult to test/mock.
 * Main purpose of this wrapper is to make testing easier.
 *
 */
@Singleton
class AppPrefsWrapper @Inject constructor() {
    companion object {
        private const val LIGHT_MODE_ID = 0
        private const val DARK_MODE_ID = 1
    }

    var isUsingV4Api: Boolean
        get() = AppPrefs.isUsingV4Api()
        set(enabled) = AppPrefs.setIsUsingV4Api(enabled)

    fun getAppWidgetSiteId(appWidgetId: Int) = AppPrefs.getStatsWidgetSelectedSiteId(appWidgetId)
    fun setAppWidgetSiteId(siteId: Long, appWidgetId: Int) = AppPrefs.setStatsWidgetSelectedSiteId(siteId, appWidgetId)
    fun removeAppWidgetSiteId(appWidgetId: Int) = AppPrefs.removeStatsWidgetSelectedSiteId(appWidgetId)

    fun getAppWidgetColor(appWidgetId: Int): WidgetColorMode? {
        return when (AppPrefs.getStatsWidgetColorModeId(appWidgetId)) {
            LIGHT_MODE_ID -> WidgetColorMode.LIGHT
            DARK_MODE_ID -> WidgetColorMode.DARK
            else -> null
        }
    }

    fun setAppWidgetColor(colorMode: WidgetColorMode, appWidgetId: Int) {
        val colorModeId = when (colorMode) {
            WidgetColorMode.LIGHT -> LIGHT_MODE_ID
            WidgetColorMode.DARK -> DARK_MODE_ID
        }
        AppPrefs.setStatsWidgetColorModeId(colorModeId, appWidgetId)
    }

    fun removeAppWidgetColorModeId(appWidgetId: Int) = AppPrefs.removeStatsWidgetColorModeId(appWidgetId)
}
