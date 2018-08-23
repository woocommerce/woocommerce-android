package com.woocommerce.android.helpers

import android.support.test.espresso.matcher.BoundedMatcher
import android.support.v7.widget.Toolbar
import android.view.View
import org.hamcrest.Description
import org.hamcrest.Matcher

object WCMatchers {
    /**
     * Matcher for matching the support toolbar title text.
     * Found on SO: https://stackoverflow.com/a/45928732
     */
    fun withToolbarTitle(textMatcher: Matcher<String>): Matcher<View> {
        return object : BoundedMatcher<View, Toolbar>(Toolbar::class.java) {
            override fun matchesSafely(toolbar: Toolbar): Boolean {
                return textMatcher.matches(toolbar.title)
            }

            override fun describeTo(description: Description) {
                description.appendText("with toolbar title: ")
                textMatcher.describeTo(description)
            }
        }
    }
}
