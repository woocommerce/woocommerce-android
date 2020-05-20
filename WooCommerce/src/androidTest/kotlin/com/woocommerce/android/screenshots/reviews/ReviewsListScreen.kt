package com.woocommerce.android.screenshots.reviews

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
}
