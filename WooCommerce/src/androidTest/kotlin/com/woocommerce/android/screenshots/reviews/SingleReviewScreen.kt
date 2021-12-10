package com.woocommerce.android.screenshots.reviews

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.CustomMatchers
import com.woocommerce.android.screenshots.util.ReviewData
import com.woocommerce.android.screenshots.util.Screen
import org.hamcrest.Matchers

class SingleReviewScreen : Screen {
    companion object {
        const val PRODUCT_NAME_LABEL = R.id.review_product_name
    }

    constructor() : super(PRODUCT_NAME_LABEL)

    fun goBackToReviewsScreen(): ReviewsListScreen {
        pressBack()
        return ReviewsListScreen()
    }

    fun assertSingleReviewScreen(review: ReviewData): SingleReviewScreen {
        // Navigation bar
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.toolbar),
                ViewMatchers.withChild(ViewMatchers.withText("Review"))
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        // Review top bar
        Espresso.onView(ViewMatchers.withId(R.id.review_product_icon))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.review_product_name))
            .check(ViewAssertions.matches(ViewMatchers.withText(review.product)))
        Espresso.onView(ViewMatchers.withContentDescription("View the product"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        // Review 'core'
        Espresso.onView(ViewMatchers.withId(R.id.review_gravatar))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.review_user_name))
            .check(ViewAssertions.matches(ViewMatchers.withText(review.reviewer)))
        Espresso.onView(ViewMatchers.withId(R.id.review_time)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.review_rating_bar))
            .check(ViewAssertions.matches(CustomMatchers().withStarsNumber(review.rating)))
        Espresso.onView(ViewMatchers.withId(R.id.review_description))
            .check(ViewAssertions.matches(ViewMatchers.withText(review.review)))
        // Flow buttons
        Espresso.onView(ViewMatchers.withId(R.id.review_trash))
            .check(ViewAssertions.matches(ViewMatchers.withText("Trash")))
        Espresso.onView(ViewMatchers.withId(R.id.review_spam))
            .check(ViewAssertions.matches(ViewMatchers.withText("Spam")))
        Espresso.onView(ViewMatchers.withId(R.id.review_approve))
            .check(ViewAssertions.matches(ViewMatchers.withText(review.approveButtonTitle)))

        return SingleReviewScreen()
    }
}
