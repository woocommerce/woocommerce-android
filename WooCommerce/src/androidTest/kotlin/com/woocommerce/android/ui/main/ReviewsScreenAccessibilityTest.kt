package com.woocommerce.android.ui.main

import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.helpers.InitializationRule
import com.woocommerce.android.helpers.TestBase
import com.woocommerce.android.screenshots.TabNavComponent
import com.woocommerce.android.screenshots.login.WelcomeScreen
import com.woocommerce.android.screenshots.reviews.ReviewsListScreen
import com.woocommerce.android.screenshots.util.MocksReader
import com.woocommerce.android.screenshots.util.ReviewData
import com.woocommerce.android.screenshots.util.iterator
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ReviewsScreenAccessibilityTest : TestBase() {
    @get:Rule(order = 0)
    val rule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val initRule = InitializationRule()

    @get:Rule(order = 2)
    var activityRule = ActivityTestRule(MainActivity::class.java)

    @Before
    fun setUp() {
        AccessibilityChecks.enable().setRunChecksFromRootView(true)
        WelcomeScreen
            .logoutIfNeeded()
            .selectLogin()
            .proceedWith(BuildConfig.SCREENSHOTS_URL)
            .proceedWith(BuildConfig.SCREENSHOTS_USERNAME)
            .proceedWith(BuildConfig.SCREENSHOTS_PASSWORD)
    }

    @Test
    fun checkReviewScreenAccessibilityTest() {
        TabNavComponent().gotoReviewsScreen()

        val reviewsJSONArray = MocksReader().readAllReviewsToArray()

        for (review in reviewsJSONArray.iterator()) {
            val currentReview = ReviewData(
                review.getInt("product_id"),
                review.getString("status"),
                review.getString("reviewer"),
                review.getString("review"),
                review.getInt("rating")
            )

            ReviewsListScreen()
                .scrollToReview(currentReview.title)
        }
    }

    @Test
    fun checkReviewDetailScreenAccessibilityTest() {
        TabNavComponent().gotoReviewsScreen()

        val reviewsJSONArray = MocksReader().readAllReviewsToArray()

        for (review in reviewsJSONArray.iterator()) {
            val currentReview = ReviewData(
                review.getInt("product_id"),
                review.getString("status"),
                review.getString("reviewer"),
                review.getString("review"),
                review.getInt("rating")
            )

            ReviewsListScreen()
                .scrollToReview(currentReview.title)
                .selectReviewByTitle(currentReview.title)
                .goBackToReviewsScreen()
        }
    }

}
