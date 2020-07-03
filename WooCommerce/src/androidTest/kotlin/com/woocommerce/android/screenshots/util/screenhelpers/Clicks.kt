package com.woocommerce.android.screenshots.util.screenhelpers

import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.screenshots.util.Screen
import com.woocommerce.android.screenshots.util.matchers.AmbiguousViewMatcher
import org.hamcrest.CoreMatchers

class Clicks {
    fun clickOn(elementID: Int) {
        Screen.waitOps.waitForElementToBeDisplayed(elementID)
        clickOn(Espresso.onView(ViewMatchers.withId(elementID)))
        Waits().idleFor(500) // allow for transitions
    }

    fun clickOn(elementID: Int, text: String) {
        Screen.waitOps.waitForAtLeastOneElementToBeDisplayed(elementID)
        clickOn(
            Espresso.onView(
                CoreMatchers.allOf(
                    ViewMatchers.withId(elementID), ViewMatchers.withText(text)
                )
            )
        )
        Waits().idleFor(500) // allow for transitions
    }

    // Use in case of AmbiguousViewMatcherException
    fun clickOn(elementID: Int, index: Int) {
        Screen.waitOps.waitForAtLeastOneElementToBeDisplayed(elementID)
        Espresso.onView(AmbiguousViewMatcher.withIndex(ViewMatchers.withId(elementID), index))
            .perform(ViewActions.click())
        Waits().idleFor(500) // allow for transitions
    }

    fun clickOn(viewInteraction: ViewInteraction) {
        Screen.waitOps.waitForElementToBeDisplayed(viewInteraction)
        Waits().idleFor(500) // allow for transitions
        viewInteraction.perform(ViewActions.click(ViewActions.closeSoftKeyboard()))
        Waits().idleFor(500) // allow for transitions
    }

    fun clickOnInDialogViewWithText(text: String) {
        Espresso.onView(ViewMatchers.withText(text))
            .inRoot(RootMatchers.isDialog())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .perform(ViewActions.click())
    }


    fun clickButtonInDialogWithTitle(resourceID: Int) {
        val title = ActivityHelper.getTranslatedString(resourceID)
        val dialogButton = Espresso.onView(ViewMatchers.withText(title)).inRoot(RootMatchers.isDialog())
        clickOn(dialogButton)
    }
}
