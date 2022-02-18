package com.woocommerce.android.screenshots.reviews

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.R
import com.woocommerce.android.screenshots.moremenu.MoreMenuScreen
import com.woocommerce.android.screenshots.util.CustomMatchers
import com.woocommerce.android.screenshots.util.ReviewData
import com.woocommerce.android.screenshots.util.Screen
import org.hamcrest.Matchers

class ReviewsListScreen : Screen(LIST_VIEW) {
    companion object {
        const val LIST_VIEW = R.id.reviewsList
    }

    fun selectReviewByTitle(reviewTitle: String): SingleReviewScreen {
        selectListItem(reviewTitle, LIST_VIEW)
        return SingleReviewScreen()
    }

    fun selectReviewByIndex(reviewIndex: Int): SingleReviewScreen {
        selectItemAtIndexInRecyclerView(reviewIndex, LIST_VIEW, LIST_VIEW)
        return SingleReviewScreen()
    }

    fun scrollToReview(reviewTitle: String): ReviewsListScreen {
        scrollToListItem(reviewTitle, LIST_VIEW)
        return ReviewsListScreen()
    }

    fun goBackToMoreMenuScreen(): MoreMenuScreen {
        pressBack()
        return MoreMenuScreen()
    }

    fun assertReviewCard(review: ReviewData): ReviewsListScreen {
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
