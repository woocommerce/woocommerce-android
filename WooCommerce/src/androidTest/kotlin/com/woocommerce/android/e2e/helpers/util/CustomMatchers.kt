package com.woocommerce.android.e2e.helpers.util

import android.view.View
import androidx.appcompat.widget.AppCompatRatingBar
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.util.TreeIterables
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

class CustomMatchers {
    fun withStarsNumber(expectedStars: Int): Matcher<View?> {
        return object : BoundedMatcher<View?, AppCompatRatingBar>(AppCompatRatingBar::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("Expected review rating: $expectedStars")
            }

            override fun matchesSafely(ratingBar: AppCompatRatingBar): Boolean {
                return ratingBar.rating == expectedStars.toFloat()
            }
        }
    }

    // Hat tip https://stackoverflow.com/a/69943394
    fun withViewCount(viewMatcher: Matcher<View>, expectedCount: Int): Matcher<View?> {
        return object : TypeSafeMatcher<View?>() {
            private var actualCount = -1
            override fun describeTo(description: Description) {
                when {
                    actualCount >= 0 -> description.also {
                        it.appendText("Expected items count: $expectedCount, but got: $actualCount")
                    }
                }
            }

            override fun matchesSafely(root: View?): Boolean {
                actualCount = TreeIterables.breadthFirstViewTraversal(root).count {
                    viewMatcher.matches(it)
                }
                return expectedCount == actualCount
            }
        }
    }
}
