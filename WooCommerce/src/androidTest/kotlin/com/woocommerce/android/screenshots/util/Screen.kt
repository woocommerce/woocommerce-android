package com.woocommerce.android.screenshots.util

import android.app.Activity
import android.view.View
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
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage.RESUMED
import com.google.android.material.tabs.TabLayout
import com.woocommerce.android.R
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matcher
import java.util.function.Supplier

open class Screen {
    private val elementID: Int

    constructor(elementID: Int) {
        this.elementID = elementID
        waitForElementToBeDisplayed(elementID)
    }

    companion object {
        fun isVisible(): Boolean {
            return false
        }
    }

    inline fun <reified T> then(closure: (T) -> Unit): T {
        closure(this as T)

        // This will fail if the `Screen` subclass constructor requires parameters.
        // For now, perhaps avoid doing that.
        return T::class.java.newInstance()
    }

    fun clickOn(elementID: Int) {
        waitForElementToBeDisplayed(elementID)
        clickOn(onView(withId(elementID)))
        idleFor(500) // allow for transitions
    }

    fun scrollTo(elementID: Int) {
        waitForElementToBeDisplayed(elementID)

        // Need to use the NestedScrollViewExtension because Espresso doesn't natively support it:
        // https://medium.com/@devasierra/espresso-nestedscrollview-scrolling-via-kotlin-delegation-5e7f0aa64c09
        onView(withId(elementID)).perform(NestedScrollViewExtension())
    }

    private fun clickOn(viewInteraction: ViewInteraction) {
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

    fun selectItemWithTitleInTabLayout(stringID: Int, tabLayout: Int, elementParentId: Int) {
        val string = getTranslatedString(stringID)
        val tabLayout = onView(allOf(
                isDescendantOfA(withId(elementParentId)),
                withId(tabLayout)
        ))

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
                        tab?.select()
                    }
                }
            }
        }
    }

    fun selectItemAtIndexInRecyclerView(index: Int, recyclerViewId: Int) {
        onView(withId(recyclerViewId))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(index, click()))
    }

    fun clickButtonInDialogWithTitle(resourceID: Int) {
        val title = getTranslatedString(resourceID)
        val dialogButton = onView(ViewMatchers.withText(title)).inRoot(isDialog())
        clickOn(dialogButton)
    }

    fun dismissSoftwareKeyboard() {
        closeSoftKeyboard()
    }

    protected fun pressBack() {
        Espresso.pressBack()
    }

    private fun waitForElementToBeDisplayed(elementID: Int) {
        waitForConditionToBeTrue(Supplier<Boolean> {
            isElementDisplayed(elementID)
        })
    }

    private fun waitForElementToBeDisplayed(element: ViewInteraction) {
        waitForConditionToBeTrue(Supplier<Boolean> {
            isElementDisplayed(element)
        })
    }

    fun isElementDisplayed(elementID: Int): Boolean {
        return isElementDisplayed(onView(withId(elementID)))
    }

    private fun isElementDisplayed(element: ViewInteraction): Boolean {
        return try {
            element.check(matches(isDisplayed()))
            true
        } catch (e: Throwable) {
            false
        }
    }

    fun elementExists(element: ViewInteraction): Boolean {
        return try {
            element.check(doesNotExist())
            false
        } catch (e: Throwable) {
            true
        }
    }

    fun isElementCompletelyDisplayed(elementID: Int): Boolean {
        return isElementCompletelyDisplayed(onView(withId(elementID)))
    }

    private fun isElementClickable(element: ViewInteraction): Boolean {
        return try {
            element.check(matches(isClickable()))
            true
        } catch (e: Throwable) {
            false
        }
    }

    private fun isElementCompletelyDisplayed(element: ViewInteraction): Boolean {
        return try {
            element.check(matches(isCompletelyDisplayed()))
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

    private fun getTranslatedString(resourceID: Int): String {
        return getCurrentActivity()!!.resources.getString(resourceID)
    }

    private fun idleFor(milliseconds: Int) {
        try {
            Thread.sleep(milliseconds.toLong())
        } catch (ex: Exception) {
            // do nothing
        }
    }
}
