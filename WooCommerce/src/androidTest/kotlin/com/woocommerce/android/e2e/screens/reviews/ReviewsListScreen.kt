package com.woocommerce.android.e2e.screens.reviews

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.CustomMatchers
import com.woocommerce.android.e2e.helpers.util.ReviewData
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.screens.moremenu.MoreMenuScreen
import org.hamcrest.Matchers

class ReviewsListScreen : Screen(R.id.reviewsList) {
    fun selectReviewByTitle(reviewTitle: String): SingleReviewScreen {
        selectListItem(reviewTitle, R.id.reviewsList)
        waitForElementToBeDisplayed(R.id.review_product_name)
        return SingleReviewScreen()
    }

    fun selectReviewByIndex(reviewIndex: Int): SingleReviewScreen {
        selectItemAtIndexInRecyclerView(reviewIndex, R.id.reviewsList, R.id.reviewsList)
        return SingleReviewScreen()
    }

    fun scrollToReview(reviewTitle: String): ReviewsListScreen {
        scrollToListItem(reviewTitle, R.id.reviewsList)
        return ReviewsListScreen()
    }

    fun goBackToMoreMenuScreen(): MoreMenuScreen {
        pressBack()
        return MoreMenuScreen()
    }

    fun assertReviewCard(review: ReviewData): ReviewsListScreen {
        // Wait for the review card to appear first. This is sometimes
        // flaky on Firebase because of low emulator performance.
        waitForElementToBeDisplayed(
            Espresso.onView(
                Matchers.allOf(
                    ViewMatchers.withId(R.id.notif_title),
                    ViewMatchers.withText(review.title)
                )
            )
        )

        // Assert that review has an expected hierarchy and content
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withChild(ViewMatchers.withId(R.id.notif_icon)),
                ViewMatchers.withChild(
                    Matchers.allOf(
                        ViewMatchers.withId(R.id.notif_title),
                        ViewMatchers.withText(review.title)
                    )
                ),
                ViewMatchers.withChild(
                    Matchers.allOf(
                        ViewMatchers.withId(R.id.notif_desc),
                        ViewMatchers.withText(review.content)
                    )
                ),
                ViewMatchers.withChild(ViewMatchers.withId(R.id.notif_rating))
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        // Assert that review has an expected rating
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.notif_rating),
                ViewMatchers.hasSibling(ViewMatchers.withText(review.title))
            )
        )
            .check(ViewAssertions.matches(CustomMatchers().withStarsNumber((review.rating))))

        return ReviewsListScreen()
    }
}
