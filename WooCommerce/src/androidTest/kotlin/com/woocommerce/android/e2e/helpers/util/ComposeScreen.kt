package com.woocommerce.android.e2e.helpers.util

import android.app.Activity
import android.content.res.Configuration
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.woocommerce.android.AppPrefs
import tools.fastlane.screengrab.Screengrab

open class ComposeScreen {
    init {
        initializeAppPrefs()
    }

    private fun initializeAppPrefs() {
        AppPrefs.init(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext)

        AppPrefs.isAIProductDescriptionTooltipDismissed = true

        AppPrefs.isNotificationsPermissionBarDismissed = true
    }

    companion object {
        var screenshotCount: Int = 0
    }

    inline fun <reified T> thenTakeScreenshot(name: String): T {
        screenshotCount += 1
        val modeSuffix = if (isDarkTheme()) "dark" else "light"
        val screenshotName = "$screenshotCount-$name-$modeSuffix"
        Screengrab.screenshot(screenshotName)
        return this as T
    }

    fun isDarkTheme(): Boolean {
        return getCurrentActivity()!!.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private var mCurrentActivity: Activity? = null
    fun getCurrentActivity(): Activity? {
        InstrumentationRegistry.getInstrumentation()
            .runOnMainSync {
                val resumedActivities: Collection<*> = ActivityLifecycleMonitorRegistry
                    .getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                mCurrentActivity = if (resumedActivities.iterator().hasNext()) {
                    resumedActivities.iterator().next() as Activity?
                } else {
                    resumedActivities.toTypedArray()[0] as Activity?
                }
            }
        return mCurrentActivity
    }
}
