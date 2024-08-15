@file:Suppress("DEPRECATION")

package com.woocommerce.android.e2e.tests.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.e2e.helpers.InitializationRule
import com.woocommerce.android.e2e.helpers.TestBase
import com.woocommerce.android.e2e.helpers.util.MocksReader
import com.woocommerce.android.e2e.helpers.util.ReviewData
import com.woocommerce.android.e2e.helpers.util.iterator
import com.woocommerce.android.e2e.rules.Retry
import com.woocommerce.android.e2e.rules.RetryTestRule
import com.woocommerce.android.e2e.screens.TabNavComponent
import com.woocommerce.android.e2e.screens.login.WelcomeScreen
import com.woocommerce.android.e2e.screens.reviews.ReviewsListScreen
import com.woocommerce.android.ui.login.LoginActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ReviewsUITest : TestBase(failOnUnmatchedWireMockRequests = false) {
    @get:Rule(order = 0)
    val rule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @get:Rule(order = 2)
    val initRule = InitializationRule()

    @get:Rule(order = 3)
    var activityRule = ActivityTestRule(LoginActivity::class.java)

    @get:Rule(order = 4)
    var retryTestRule = RetryTestRule()

    @Before
    fun setUp() {
        WelcomeScreen
            .skipCarouselIfNeeded()
            .selectLogin()
            .proceedWith(BuildConfig.SCREENSHOTS_URL)
            .proceedWith(BuildConfig.SCREENSHOTS_USERNAME)
            .proceedWith(BuildConfig.SCREENSHOTS_PASSWORD)

        TabNavComponent()
            .gotoMoreMenuScreen()
            .openReviewsListScreen(composeTestRule)
    }

    @Retry(numberOfTimes = 2)
    @Test
    fun e2eReviewListShowsAllReviews() {
        val reviewsJSONArray = MocksReader().readAllReviewsToArray()

        reviewsJSONArray.iterator().forEach { review ->
            val currentReview = ReviewData(
                review.getInt("product_id"),
                review.getString("status"),
                review.getString("reviewer"),
                review.getString("review"),
                review.getInt("rating")
            )

            ReviewsListScreen()
                .scrollToReview(currentReview.title)
                .assertReviewCard(currentReview)
                .selectReviewByTitle(currentReview.title)
                .assertSingleReviewScreen(currentReview)
                .goBackToReviewsScreen()
        }
    }
}
