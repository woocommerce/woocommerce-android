package com.woocommerce.android.helpers

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.support.test.espresso.matcher.BoundedMatcher
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.TextView
import com.woocommerce.android.widgets.FlowLayout
import org.hamcrest.Description
import org.hamcrest.Matcher
import android.support.test.espresso.action.ScrollToAction
import android.support.test.espresso.UiController
import android.support.v4.widget.NestedScrollView
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA
import android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import android.support.test.espresso.ViewAction
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom
import android.widget.ListView
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.core.AnyOf.anyOf

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

    fun withTagTextColor(context: Context, color: Int): Matcher<View> {
        return object : BoundedMatcher<View, FlowLayout>(FlowLayout::class.java) {
            public override fun matchesSafely(view: FlowLayout): Boolean {
                val child = view.getChildAt(0)
                return if (child != null && child is TextView) {
                    ContextCompat.getColor(context, color) == child.textColors.defaultColor
                } else false
            }

            override fun describeTo(description: Description) {
                description.appendText("with text color: ")
            }
        }
    }

    fun withTagBackgroundColor(context: Context, color: Int): Matcher<View> {
        return object : BoundedMatcher<View, FlowLayout>(FlowLayout::class.java) {
            public override fun matchesSafely(view: FlowLayout): Boolean {
                val child = view.getChildAt(0)
                return if (child != null && child is TextView) {
                    ContextCompat.getColor(context, color) == (child.background as GradientDrawable).color.defaultColor
                } else false
            }

            override fun describeTo(description: Description) {
                description.appendText("with text color: ")
            }
        }
    }

    fun withTagText(string: String): Matcher<View> {
        return object : BoundedMatcher<View, FlowLayout>(FlowLayout::class.java) {
            public override fun matchesSafely(view: FlowLayout): Boolean {
                val child = view.getChildAt(0)
                return if (child != null && child is TextView) {
                    child.text.toString() == string
                } else false
            }

            override fun describeTo(description: Description) {
                description.appendText("with child text: ")
            }
        }
    }

    fun scrollTo(): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return allOf(
                        withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE), isDescendantOfA(
                        anyOf(
                                isAssignableFrom(ScrollView::class.java),
                                isAssignableFrom(HorizontalScrollView::class.java),
                                isAssignableFrom(NestedScrollView::class.java)
                        )
                ))
            }

            override fun getDescription(): String? {
                return null
            }

            override fun perform(uiController: UiController, view: View) {
                ScrollToAction().perform(uiController, view)
            }
        }
    }

    fun correctNumberOfItems(itemsCount: Int): Matcher<View> {
        return object : BoundedMatcher<View, ListView>(ListView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("with number of items: $itemsCount")
            }

            override fun matchesSafely(listView: ListView): Boolean {
                val adapter = listView.getAdapter()
                return adapter.count == itemsCount
            }
        }
    }
}
