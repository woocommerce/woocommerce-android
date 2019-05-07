package com.woocommerce.android.helpers

import android.support.test.espresso.matcher.BoundedMatcher
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.ListView
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

    /**
     * Matcher to check if the listView count matches
     * the incoming count value
     */
    fun withItemCount(itemsCount: Int): Matcher<View> {
        return object : BoundedMatcher<View, ListView>(ListView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("with number of items: $itemsCount")
            }

            override fun matchesSafely(listView: ListView): Boolean {
                val adapter = listView.adapter
                return adapter.count == itemsCount
            }
        }
    }
}
