package com.woocommerce.android.screenshots.util.screenhelpers

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.screenshots.util.Screen
import org.hamcrest.CoreMatchers

class Typers {
    fun typeTextInto(elementID: Int, text: String) {
        Screen.waitOps.waitForElementToBeDisplayed(elementID)
        Espresso.onView(ViewMatchers.withId(elementID))
            .perform(ViewActions.replaceText(text))
            .perform(ViewActions.closeSoftKeyboard())
    }

    fun typeTextInto(elementID: Int, hint: String, text: String) {
        Screen.waitOps.waitForAtLeastOneElementToBeDisplayed(elementID)
        Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(elementID), ViewMatchers.withHint(hint)))
            .perform(ViewActions.replaceText(text))
            .perform(ViewActions.closeSoftKeyboard())
    }
}
