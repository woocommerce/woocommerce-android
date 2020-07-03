package com.woocommerce.android.screenshots.util

import android.content.res.Configuration
import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.woocommerce.android.R.id
import com.woocommerce.android.screenshots.util.screenhelpers.Actions
import com.woocommerce.android.screenshots.util.screenhelpers.ActivityHelper
import com.woocommerce.android.screenshots.util.screenhelpers.Clicks
import com.woocommerce.android.screenshots.util.screenhelpers.Selectors
import com.woocommerce.android.screenshots.util.screenhelpers.Typers
import com.woocommerce.android.screenshots.util.screenhelpers.Waits
import org.hamcrest.CoreMatchers
import tools.fastlane.screengrab.Screengrab

open class Screen// If we fail to find the element, attempt recovery
(elementID: Int) {
    open fun recover(): Unit = Unit

    init {
        if (!waitOps.waitForElementToBeDisplayedWithoutFailure(elementID)) {
            recover()
            waitOps.waitForElementToBeDisplayed(elementID)
        }
    }

    companion object {
        var screenshotCount: Int = 0

        fun isVisible(): Boolean {
            return false
        }

        // Defined here so that it can be used even if we don't have an instance of `Screen` yet, which is handy when we
        // want to check for certain properties of the screen the app has been launched with, without knowing which
        // screen we are dealing with.
        fun isElementDisplayed(elementID: Int): Boolean {
            return isElementDisplayed(onView(withId(elementID)))
        }

        fun isElementDisplayed(element: ViewInteraction): Boolean {
            return try {
                element.check(matches(isDisplayed()))
                true
            } catch (e: Throwable) {
                false
            }
        }

        // Init helpers here
        val waitOps = Waits()
        val clickOps = Clicks()
        val typeOps = Typers()
        val selectOps = Selectors()
        val actionOps = Actions()
        val activityHelper = ActivityHelper

        fun isLoggedOut(): Boolean {
            return try {
                isElementDisplayed(id.dashboard)
                false
            } catch (e: Throwable) {
                true
            }
        }

        open fun isToolbarTitle(title: String): Boolean {
            return try {
                onView(
                    CoreMatchers.allOf(
                        CoreMatchers.instanceOf(TextView::class.java),
                        ViewMatchers.withParent(withId(id.toolbar))
                    )
                ).check(matches(ViewMatchers.withText(title)))
                true
            } catch (e: Throwable) {
                false
            }
        }
    }

    inline fun <reified T> then(closure: (T) -> Unit): T {
        closure(this as T)
        return this
    }

    inline fun <reified T> thenTakeScreenshot(name: String): T {
        screenshotCount += 1
        val modeSuffix = if (isDarkTheme()) "dark" else "light"
        val screenshotName = "$screenshotCount-$name-$modeSuffix"
//        try {
        Screengrab.screenshot(screenshotName)
//        } catch (e: Throwable) {
//            Log.w("screenshots", "Error capturing $screenshotName", e)
//        }
        return this as T
    }

    // BOOLEANS

    fun isLoggedIn(): Boolean {
        return try {
            isElementDisplayed(id.dashboard)
            true
        } catch (e: Throwable) {
            false
        }
    }

    fun isDarkTheme(): Boolean {
        return activityHelper.getCurrentActivity()!!.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    fun isDisplayingDialog(): Boolean {
        val dialog = onView(withId(id.button1))
            .inRoot(isDialog())

        return isElementDisplayed(dialog)
    }

    fun isElementDisplayed(elementID: Int): Boolean {
        return Screen.isElementDisplayed(elementID)
    }

    fun isElementCompletelyDisplayed(elementID: Int): Boolean {
        return isElementCompletelyDisplayed(onView(withId(elementID)))
    }

    private fun isElementCompletelyDisplayed(element: ViewInteraction): Boolean {
        return try {
            element.check(matches(isCompletelyDisplayed()))
            true
        } catch (e: Throwable) {
            false
        }
    }
}
