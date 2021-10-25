package com.woocommerce.android.screenshots.reviews

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.R
import com.woocommerce.android.screenshots.TabNavComponent
import com.woocommerce.android.screenshots.util.CustomMatchers
import com.woocommerce.android.screenshots.util.ReviewData
import com.woocommerce.android.screenshots.util.Screen
import org.hamcrest.Matchers

class ReviewsListScreen : Screen {
    companion object {
        const val LIST_VIEW = R.id.reviewsList

        const val REVIEW_ICON = R.id.notif_icon
    }

    val tabBar = TabNavComponent()

    constructor() : super(LIST_VIEW)

    fun selectReview(index: Int): SingleReviewScreen {
        val correctedIndex = index + 1 // account for the header
        selectItemAtIndexInRecyclerView(correctedIndex, LIST_VIEW, REVIEW_ICON)
        return SingleReviewScreen()
    }

    fun selectReviewByTitle(reviewTitle: String): SingleReviewScreen {
        Espresso.onView(ViewMatchers.withId(LIST_VIEW)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                ViewMatchers.hasDescendant(
                    ViewMatchers.withText(
                        reviewTitle
                    )
                ),
                ViewActions.click()
            )
        )

        return SingleReviewScreen()
    }

    fun scrollToReview(reviewTitle: String): ReviewsListScreen {
        Espresso.onView(ViewMatchers.withId(LIST_VIEW)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                ViewMatchers.hasDescendant(
                    ViewMatchers.withText(
                        reviewTitle
                    )
                ),
                ViewActions.scrollTo()
            )
        )

        return ReviewsListScreen()
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
