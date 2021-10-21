package com.woocommerce.android.screenshots.reviews

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.R
import com.woocommerce.android.screenshots.TabNavComponent
import com.woocommerce.android.screenshots.util.Screen

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
                ), ViewActions.click()
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
                ), ViewActions.scrollTo()
            )
        )

        return ReviewsListScreen()
    }
}
