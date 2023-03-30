package com.woocommerce.android.e2e.helpers.util

import android.app.Activity
import android.content.res.Configuration
import android.view.View
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage.RESUMED
import com.google.android.material.tabs.TabLayout
import com.woocommerce.android.R
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.`is`
import tools.fastlane.screengrab.Screengrab
import java.util.function.Supplier

open class Screen {
    private val elementID: Int

    constructor(elementID: Int) {
        this.elementID = elementID

        // If we fail to find the element, attempt recovery
        if (!waitForElementToBeDisplayedWithoutFailure(elementID)) {
            recover()
            waitForElementToBeDisplayed(elementID)
        }
    }

    open fun recover() = Unit

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

        fun isElementDisplayed(text: String): Boolean {
            return isElementDisplayed(onView(withText(text)))
        }

        private fun isElementDisplayed(element: ViewInteraction): Boolean {
            return try {
                element.check(matches(isDisplayed()))
                true
            } catch (e: Throwable) {
                false
            }
        }

        private fun isElementNotDisplayed(element: Int): Boolean {
            return try {
                onView(withId(element))
                    .check(matches(not(isDisplayed())))
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
        // Wait for 2 seconds to increase the probability of all
        // screen components to be loaded properly
        idleFor(2000)
        Screengrab.screenshot(screenshotName)
//        } catch (e: Throwable) {
//            Log.w("screenshots", "Error capturing $screenshotName", e)
//        }
        return this as T
    }

    fun isDarkTheme(): Boolean {
        return getCurrentActivity()!!.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    fun clickOn(elementID: Int) {
        waitForElementToBeDisplayed(elementID)
        clickOn(onView(withId(elementID)))
        idleFor(500) // allow for transitions
    }

    fun clickByTextAndId(text: String, id: Int, ) {
        val element = onView(
            Matchers.allOf(
                withText(text),
                withId(id)
            )
        )

        clickOn(element)
    }

    fun scrollTo(elementID: Int) {
        waitForElementToBeDisplayed(elementID)

        // Need to use the NestedScrollViewExtension because Espresso doesn't natively support it:
        // https://medium.com/@devasierra/espresso-nestedscrollview-scrolling-via-kotlin-delegation-5e7f0aa64c09
        onView(withId(elementID)).perform(NestedScrollViewExtension())
    }

    fun scrollToListItem(itemTitle: String, listID: Int) {
        Espresso.onView(ViewMatchers.withId(listID)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                ViewMatchers.hasDescendant(
                    ViewMatchers.withText(
                        itemTitle
                    )
                ),
                ViewActions.scrollTo()
            )
        )
    }

    fun scrollToListItem(itemTitle: String, list: ViewInteraction) {
        list.perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                ViewMatchers.hasDescendant(
                    ViewMatchers.withText(
                        itemTitle
                    )
                ),
                ViewActions.scrollTo()
            )
        )
    }

    fun selectListItem(itemTitle: String, listID: Int) {
        Espresso.onView(ViewMatchers.withId(listID)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                ViewMatchers.hasDescendant(
                    ViewMatchers.withText(
                        itemTitle
                    )
                ),
                ViewActions.click()
            )
        )

        idleFor(1000) // allow for transitions
    }

    fun clickOn(viewInteraction: ViewInteraction) {
        waitForElementToBeDisplayed(viewInteraction)
        idleFor(500) // allow for transitions
        viewInteraction.perform(ViewActions.click(ViewActions.closeSoftKeyboard()))
        idleFor(500) // allow for transitions
    }

    fun typeTextInto(elementID: Int, text: String) {
        waitForElementToBeDisplayed(elementID)
        onView(withId(elementID))
            .perform(ViewActions.replaceText(text))
            .perform(ViewActions.closeSoftKeyboard())
    }

    fun openToolbarActionMenu() {
        waitForElementToBeDisplayed(R.id.toolbar)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        openActionBarOverflowOrOptionsMenu(context)
    }

    fun flipSwitchOn(elementID: Int, elementParentId: Int = 0) {
        if (elementParentId == 0) {
            onView(withId(elementID))
                .check(matches(isNotChecked()))
                .perform(click())
        } else {
            onView(allOf(isDescendantOfA(withId(elementParentId)), withId(elementID)))
                .check(matches(isNotChecked()))
                .perform(click())
        }
    }

    fun selectItemWithTitleInTabLayout(stringID: Int, tabLayoutId: Int, elementParentId: Int) {
        val string = getTranslatedString(stringID)
        val tabLayout = onView(
            allOf(
                isDescendantOfA(withId(elementParentId)),
                withId(tabLayoutId)
            )
        )

        tabLayout.perform(selectTabWithText(string))
    }

    fun selectItemWithTitleInTabLayout(stringID: Int, elementParentId: Int) {
        val string = getTranslatedString(stringID)
        val tabLayout = onView(
            allOf(
                isDescendantOfA(withId(elementParentId)),
                withClassName(`is`("com.google.android.material.tabs.TabLayout"))
            )
        )

        tabLayout.perform(selectTabWithText(string))
    }

    private fun selectTabWithText(string: String): ViewAction {
        return object : ViewAction {
            override fun getDescription() = "with tab having title $string"

            override fun getConstraints(): Matcher<View>? {
                return allOf(
                    isDisplayed(),
                    isAssignableFrom(TabLayout::class.java)
                )
            }

            override fun perform(uiController: UiController, view: View) {
                val tabLayout = view as TabLayout
                tabLayout.tabCount

                for (i in 0 until tabLayout.tabCount) {
                    val tab = tabLayout.getTabAt(i)
                    if (tab?.text == string) {
                        tab.select()
                    }
                }
            }
        }
    }

    fun selectItemAtIndexInRecyclerView(index: Int, recyclerViewId: Int, elementId: Int = 0) {
        if (elementId == 0) {
            waitForAtLeastOneElementToBeDisplayed(elementId)
        }

        idleFor(1000)

        onView(withId(recyclerViewId))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(index, click()))
    }

    fun clickButtonInDialogWithTitle(@StringRes resourceID: Int) {
        val title = getTranslatedString(resourceID)
        clickButtonInDialogWithTitle(title)
    }

    fun clickButtonInDialogWithTitle(title: String) {
        val dialogButton = onView(ViewMatchers.withText(title)).inRoot(isDialog())
        clickOn(dialogButton)
    }

    fun dismissSoftwareKeyboard() {
        closeSoftKeyboard()
    }

    protected fun pressBack() {
        Espresso.pressBack()
        idleFor(1000) // allow for transitions
    }

    fun waitForElementToBeDisplayed(elementID: Int) {
        waitForConditionToBeTrue(
            Supplier<Boolean> {
                isElementDisplayed(elementID)
            }
        )
    }

    fun waitForElementToBeDisplayed(element: ViewInteraction) {
        waitForConditionToBeTrue(
            Supplier<Boolean> {
                isElementDisplayed(element)
            }
        )
    }

    fun waitForElementToBeDisplayedWithoutFailure(elementId: Int): Boolean {
        try {
            waitForConditionToBeTrueWithoutFailure(
                Supplier<Boolean> {
                    isElementDisplayed(elementId)
                }
            )
        } catch (e: java.lang.Exception) { // ignore the failure
        }
        return isElementDisplayed(elementId)
    }

    fun waitForElementToDisappear(element: Int) {
        waitForConditionToBeTrue(
            Supplier<Boolean> {
                isElementNotDisplayed(element)
            }
        )
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

    // HELPERS
    private fun atLeastOneElementIsDisplayed(elementId: Int): Boolean {
        return try {
            onView(
                allOf(
                    withId(elementId),
                    first()
                )
            ).check(matches(isDisplayed()))
            true
        } catch (e: Throwable) {
            false
        }
    }

    private fun waitForConditionToBeTrue(supplier: Supplier<Boolean>) {
        if (supplier.get()) {
            return
        }
        SupplierIdler(supplier).idleUntilReady()
    }

    private fun waitForConditionToBeTrueWithoutFailure(supplier: Supplier<Boolean>) {
        if (supplier.get()) {
            return
        }
        SupplierIdler(supplier).idleUntilReady(false)
    }

    private fun waitForAtLeastOneElementToBeDisplayed(elementId: Int) {
        waitForConditionToBeTrue(
            Supplier {
                atLeastOneElementIsDisplayed(elementId)
            }
        )
    }

    // MATCHERS
    private fun first(): FirstMatcher {
        return FirstMatcher()
    }

    private var mCurrentActivity: Activity? = null
    private fun getCurrentActivity(): Activity? {
        InstrumentationRegistry.getInstrumentation()
            .runOnMainSync {
                val resumedActivities: Collection<*> = ActivityLifecycleMonitorRegistry
                    .getInstance()
                    .getActivitiesInStage(RESUMED)
                mCurrentActivity = if (resumedActivities.iterator().hasNext()) {
                    resumedActivities.iterator().next() as Activity?
                } else {
                    resumedActivities.toTypedArray()[0] as Activity?
                }
            }
        return mCurrentActivity
    }

    protected fun getTranslatedString(resourceID: Int): String {
        return getCurrentActivity()!!.resources.getString(resourceID)
    }

    fun idleFor(milliseconds: Int) {
        try {
            Thread.sleep(milliseconds.toLong())
        } catch (ex: Exception) {
            // do nothing
        }
    }
}
