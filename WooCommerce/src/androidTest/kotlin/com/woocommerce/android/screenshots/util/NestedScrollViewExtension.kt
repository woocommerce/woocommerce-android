package com.woocommerce.android.screenshots.util

import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ListView
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher
import org.hamcrest.Matchers

class NestedScrollViewExtension(scrollToAction: ViewAction = ViewActions.scrollTo()) : ViewAction by scrollToAction {
    override fun getConstraints(): Matcher<View> {
        return Matchers.allOf(
                ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
                ViewMatchers.isDescendantOfA(Matchers.anyOf(ViewMatchers.isAssignableFrom(NestedScrollView::class.java),
                        ViewMatchers.isAssignableFrom(ScrollView::class.java),
                        ViewMatchers.isAssignableFrom(HorizontalScrollView::class.java),
                        ViewMatchers.isAssignableFrom(ListView::class.java))))
    }
}
