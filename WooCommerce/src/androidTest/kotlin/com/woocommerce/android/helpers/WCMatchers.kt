package com.woocommerce.android.helpers

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatRatingBar
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ScrollToAction
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import com.woocommerce.android.widgets.FlowLayout
import com.woocommerce.android.widgets.sectionedrecyclerview.SectionedRecyclerViewAdapter
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
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

    /**
     * Custom matcher to check if the [FlowLayout] has a child view
     * which is a [TextView] and matches the [TextView] text color
     * with the incoming color value
     */
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

    /**
     * Custom matcher to check if the [FlowLayout] has a child view
     * which is a [TextView] and matches the [TextView] background color
     * with the incoming color value
     */
    fun withTagBackgroundColor(context: Context, color: Int): Matcher<View> {
        return object : BoundedMatcher<View, FlowLayout>(FlowLayout::class.java) {
            public override fun matchesSafely(view: FlowLayout): Boolean {
                val child = view.getChildAt(0)
                return if (child != null && child is TextView) {
                    (child.background as GradientDrawable).color?.let { bgColor ->
                        ContextCompat.getColor(context, color) == bgColor.defaultColor
                    } ?: false
                } else false
            }

            override fun describeTo(description: Description) {
                description.appendText("with text color: ")
            }
        }
    }

    /**
     * Custom matcher to check if the [FlowLayout] has a child view
     * which is a [TextView] and matches the [TextView] text
     * with the incoming String value
     */
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

    /**
     * Custom matcher to scroll to the bottom of the
     * view specified before performing an action on the view
     * Since the view might not be visible, scrollTo()
     * is used to this ensures that the view is displayed before
     * proceeding to the click() action
     */
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

    /**
     * Custom matcher to check if the [SectionedRecyclerViewAdapter] section count matches
     * the incoming count value
     */
    fun withSectionCount(itemsCount: Int): Matcher<View> {
        return object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("with number of items: $itemsCount")
            }

            override fun matchesSafely(recyclerView: RecyclerView): Boolean {
                val adapter = recyclerView.adapter as? SectionedRecyclerViewAdapter
                return adapter?.getSectionTotal() == itemsCount
            }
        }
    }

    /**
     * Returns a custom recyclerview matcher class for RecyclerView to
     * perform actions and matches on list items by position.
     */
    fun withRecyclerView(recyclerViewId: Int): RecyclerViewMatcher {
        return RecyclerViewMatcher(recyclerViewId)
    }

    /**
     * Matcher to check if the textView text color matches
     * the incoming color
     */
    fun withTextColor(expectedId: Int): Matcher<View> {
        return object : BoundedMatcher<View, TextView>(TextView::class.java) {
            override fun matchesSafely(textView: TextView): Boolean {
                val colorId = ContextCompat.getColor(textView.context, expectedId)
                return textView.currentTextColor == colorId
            }

            override fun describeTo(description: Description) {
                description.appendText("with text color: ")
                description.appendValue(expectedId)
            }
        }
    }

    /**
     * Matcher to check if the ImageView drawable matches
     * the incoming drawable resource Id
     */
    fun withDrawable(resourceId: Int): Matcher<View> {
        return DrawableMatcher(resourceId)
    }

    /**
     * Matcher to check if the Textview/EditText/Button
     * have error text that matches the incoming string
     */
    fun matchesError(error: String): Matcher<View> {
        return object : BoundedMatcher<View, TextView>(TextView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("has error text: ")
            }

            override fun matchesSafely(view: TextView): Boolean {
                return view.error == error
            }
        }
    }

    /**
     * Matcher to check if the Textview/EditText/Button
     * have no error text and it is set to null
     */
    fun matchesHasNoErrorText(): Matcher<View> {
        return object : BoundedMatcher<View, TextView>(TextView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("has no error text: ")
            }

            override fun matchesSafely(view: TextView): Boolean {
                return view.error == null
            }
        }
    }

    /**
     * Matcher to get the particular view for a given id and get the view at a particular index provided.
     * For instance, in Product Detail page, we have multiple dynamic views for the same view ID.
     * Using this method, we can choose which view we want without causing an AmbiguousViewMatcherException
     * Reference: https://stackoverflow.com/a/39756832/9862062
     */
    fun matchesWithIndex(matcher: Matcher<View>, index: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            var currentIndex = 0

            override fun describeTo(description: Description) {
                description.appendText("with index: ")
                description.appendValue(index)
                matcher.describeTo(description)
            }

            override fun matchesSafely(view: View): Boolean {
                return matcher.matches(view) && currentIndex++ == index
            }
        }
    }

    /**
     * Matcher to check if the RatingBar have rating text that matches the incoming value
     */
    fun matchesRating(rating: Float): Matcher<View> {
        return object : BoundedMatcher<View, AppCompatRatingBar>(AppCompatRatingBar::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("RatingBar should have $rating")
            }

            override fun matchesSafely(view: AppCompatRatingBar): Boolean {
                return view.rating == rating
            }
        }
    }
}
