package com.woocommerce.android.ui.main

import android.view.View
import androidx.appcompat.widget.AppCompatRatingBar
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.R
import com.woocommerce.android.helpers.InitializationRule
import com.woocommerce.android.helpers.TestBase
import com.woocommerce.android.screenshots.TabNavComponent
import com.woocommerce.android.screenshots.login.WelcomeScreen
import com.woocommerce.android.screenshots.reviews.ReviewsListScreen
import com.woocommerce.android.screenshots.reviews.SingleReviewScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.json.JSONObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test

val productsMap = mapOf(
    2132 to "Rose Gold shades",
    2131 to "Colorado shades",
    2130 to "Black Coral shades"
)

data class Review(
    val productID: Int,
    val status: String,
    val reviewer: String,
    val review: String,
    val rating: Int
) {
    val product = productsMap[productID]
    val title = "$reviewer left a review on $product"
    val content = if (status == "hold") "Pending Review • $review" else review
    val approveButtonTitle = if (status == "hold") "Approve" else "Approved"
}

@HiltAndroidTest
class ReviewsUITest : TestBase() {
    @get:Rule(order = 0)
    val rule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val initRule = InitializationRule()

    @get:Rule(order = 3)
    var activityRule = ActivityTestRule(MainActivity::class.java)

    @Before
    fun setUp() {
        WelcomeScreen
            .logoutIfNeeded()
            .selectLogin()
            .proceedWith(BuildConfig.SCREENSHOTS_URL)
            .proceedWith(BuildConfig.SCREENSHOTS_USERNAME)
            .proceedWith(BuildConfig.SCREENSHOTS_PASSWORD)

        TabNavComponent().gotoReviewsScreen()
    }

    @Test
    fun reviewListShowsAllReviews() {
        val str = readAssetsFile("mocks/mappings/jetpack-blogs/wc/reviews/products_reviews_all.json")
        val json = JSONObject(str)
        val reviews = json.getJSONObject("response").getJSONObject("jsonBody").getJSONArray("data")

        for (i in 0 until reviews.length()) {
            val reviewContainer: JSONObject = reviews.getJSONObject(i)
            val currentReview = Review(
                reviewContainer.getInt("product_id"),
                reviewContainer.getString("status"),
                reviewContainer.getString("reviewer"),
                reviewContainer.getString("review"),
                reviewContainer.getInt("rating")
            )

            Thread.sleep(3000)
            ReviewsListScreen().scrollToReview(currentReview.title)
            assertReviewCard(currentReview)

            ReviewsListScreen().selectReviewByTitle(currentReview.title)
            assertSingleReviewScreen(currentReview)
            SingleReviewScreen().goBackToReviewsScreen()
        }

        Thread.sleep(10000000)
    }

    private fun assertReviewCard(review: Review) {
        onView(
            allOf(
                withChild(withId(R.id.notif_icon)),
                withChild(allOf(withId(R.id.notif_title), withText(review.title))),
                withChild(allOf(withId(R.id.notif_desc), withText(review.content))),
                withChild(withId(R.id.notif_rating))
            )
        )
            .check(matches(isDisplayed()))

        // Assert that a specific review has an expected rating
        onView(
            allOf(
                withId(R.id.notif_rating),
                hasSibling(withText(review.title))
            )
        )
            .check(matches(withStarsNumber(review.rating)))
    }

    private fun assertSingleReviewScreen(review: Review) {
        // Navigation bar
        onView(
            allOf(
                withId(R.id.toolbar),
                withChild(withContentDescription("Navigate up")),
                withChild(withText("Review"))
            )
        )
            .check(matches(isDisplayed()))

        onView(withContentDescription("Navigate up")).check(matches(isDisplayed()))
        // Review top bar
        onView(withId(R.id.review_product_icon)).check(matches(isDisplayed()))
        onView(withId(R.id.review_product_name)).check(matches(withText(review.product)))
        onView(withContentDescription("View the product")).check(matches(isDisplayed()))
        // Review 'core'
        onView(withId(R.id.review_gravatar)).check(matches(isDisplayed()))
        onView(withId(R.id.review_user_name)).check(matches(withText(review.reviewer)))
        onView(withId(R.id.review_time)).check(matches(isDisplayed()))
        onView(withId(R.id.review_rating_bar)).check(matches(withStarsNumber(review.rating)))
        onView(withId(R.id.review_description)).check(matches(withText(review.review)))
        // Flow buttons
        onView(withId(R.id.review_trash)).check(matches(withText("Trash")))
        onView(withId(R.id.review_spam)).check(matches(withText("Spam")))
        onView(withId(R.id.review_approve)).check(matches(withText(review.approveButtonTitle)))
    }

    private fun withStarsNumber(expectedStars: Int): Matcher<View?> {
        return object : BoundedMatcher<View?, AppCompatRatingBar>(AppCompatRatingBar::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("Expected review rating: $expectedStars")
            }

            override fun matchesSafely(ratingBar: AppCompatRatingBar): Boolean {
                return ratingBar.rating == expectedStars.toFloat()
            }
        }
    }

    private fun readAssetsFile(fileName: String): String {
        val appContext = InstrumentationRegistry.getInstrumentation().context
        return appContext.assets.open(fileName).bufferedReader().use { it.readText() }
    }
}
