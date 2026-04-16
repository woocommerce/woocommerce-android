package com.woocommerce.android.e2e.screens.reviews

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.ReviewData
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.screens.moremenu.MoreMenuScreen

class ReviewsListScreen(private val composeTestRule: ComposeTestRule) : Screen(R.id.toolbar) {
    fun selectReviewByTitle(reviewTitle: String): SingleReviewScreen {
        val reviewTitleMatcher = hasText(reviewTitle, substring = true).and(hasClickAction())

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(reviewTitleMatcher)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onAllNodes(reviewTitleMatcher)
            .onFirst()
            .assertHasClickAction()
            .performClick()

        waitForElementToBeDisplayed(R.id.review_product_name)
        return SingleReviewScreen(composeTestRule)
    }

    fun scrollToReview(reviewTitle: String): ReviewsListScreen {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasScrollToNodeAction())
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule
            .onAllNodes(hasScrollToNodeAction())
            .onFirst()
            .performScrollToNode(hasText(reviewTitle, substring = true).and(hasClickAction()))

        return this
    }

    fun goBackToMoreMenuScreen(): MoreMenuScreen {
        pressBack()
        return MoreMenuScreen()
    }

    fun assertReviewCard(review: ReviewData): ReviewsListScreen {
        // The clickable Column in ReviewListItem causes Compose to merge all descendant text
        // into one semantics node. Title and content are not siblings — they share a single
        // merged node. Match on both to uniquely identify the correct card.
        val reviewCardMatcher = hasText(review.title, substring = true)
            .and(hasText(review.content, substring = true))

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(reviewCardMatcher)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onAllNodes(reviewCardMatcher)
            .onFirst()
            .assertIsDisplayed()

        return this
    }
}
