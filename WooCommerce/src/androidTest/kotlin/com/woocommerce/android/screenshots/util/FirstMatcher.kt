package com.woocommerce.android.screenshots.util

import android.view.View
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class FirstMatcher : TypeSafeMatcher<View>(View::class.java) {
    private var hasMatched = false
    override fun matchesSafely(item: View): Boolean {
        if (hasMatched) {
            return false
        }
        hasMatched = true
        return true
    }

    override fun describeTo(description: Description) {
        description.appendText("first instance.")
    }
}
