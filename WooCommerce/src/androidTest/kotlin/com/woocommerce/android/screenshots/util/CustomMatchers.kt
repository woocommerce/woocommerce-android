package com.woocommerce.android.screenshots.util

import android.view.View
import androidx.appcompat.widget.AppCompatRatingBar
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher

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
}
