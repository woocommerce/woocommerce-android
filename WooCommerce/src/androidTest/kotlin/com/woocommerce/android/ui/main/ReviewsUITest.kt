package com.woocommerce.android.ui.main

import android.view.View
import androidx.appcompat.widget.AppCompatRatingBar
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test


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

        //val str = readFileText("mocks/mappings/jetpack-blogs/wc/reviews/products_reviews_all.json")
        //println(str)
        //Thread.sleep(1000000)

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

        val statuses = arrayOf("hold", "approved")
        val reviewers = arrayOf("Paytin Lubin", "Marcus Curtis")
        val products = arrayOf("Rose Gold shades", "Black Coral shades")
        val reviews = arrayOf("Travel in style!", "Don't really like them! Thought they are waterproof!")
        val ratings = arrayOf(5, 2)

        /*
        val status = "approved"
        val reviewer = "Marcus Curtis"
        val product = "Black Coral shades"
        val review = "Don't really like them! Thought they are waterproof!"
        val rating = 2
        */

        for (i in 0..statuses.size-1) {

            val status = statuses[i]
            val reviewer = reviewers[i]
            val product = products[i]
            val review = reviews[i]
            val rating = ratings[i]

            val reviewTitle = "$reviewer left a review on $product"
            val reviewContent = if (status == "hold") "Pending Review • $review" else review
            val reviewApproveButtonTitle = if (status == "hold") "Approve" else "Approved"

            Thread.sleep(3000)

            // Assert a review card by its hierarchy and content
            val reviewCard: ViewInteraction = onView(
                allOf(
                    withChild(withId(R.id.notif_icon)),
                    withChild(allOf(withId(R.id.notif_title), withText(reviewTitle))),
                    withChild(allOf(withId(R.id.notif_desc), withText(reviewContent))),
                    withChild(withId(R.id.notif_rating))
                )
            )
                .check(matches(isDisplayed()))

            // Assert that a specific review has an expected rating
            onView(
                allOf(
                    withId(R.id.notif_rating),
                    hasSibling(withText(reviewTitle))
                )
            )
                .check(matches(withStarsNumber(rating)))

            ReviewsListScreen().selectReviewByTitle(reviewTitle)

            // Assert separate review screen:
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
            onView(withId(R.id.review_product_name)).check(matches(withText(product)))
            onView(withContentDescription("View the product")).check(matches(isDisplayed()))
            // Review 'core'
            onView(withId(R.id.review_gravatar)).check(matches(isDisplayed()))
            onView(withId(R.id.review_user_name)).check(matches(withText(reviewer)))
            onView(withId(R.id.review_time)).check(matches(isDisplayed()))
            onView(withId(R.id.review_rating_bar)).check(matches(withStarsNumber(rating)))
            onView(withId(R.id.review_description)).check(matches(withText(review)))
            // Flow buttons
            onView(withId(R.id.review_trash)).check(matches(withText("Trash")))
            onView(withId(R.id.review_spam)).check(matches(withText("Spam")))
            onView(withId(R.id.review_approve)).check(matches(withText(reviewApproveButtonTitle)))

            SingleReviewScreen().goBackToReviewsScreen()
        }

        Thread.sleep(10000000)
    }

    private fun withStarsNumber(expectedStars: Int): Matcher<View?>? {
        return object : BoundedMatcher<View?, AppCompatRatingBar>(AppCompatRatingBar::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("Expected review rating: $expectedStars")
            }

            override fun matchesSafely(ratingBar: AppCompatRatingBar): Boolean {
                return ratingBar.rating == expectedStars.toFloat()
            }
        }
    }

    fun readAssetsFile(fileName: String): String {
        val appContext = InstrumentationRegistry.getInstrumentation().context
        return appContext.assets.open(fileName).bufferedReader().use { it.readText() }
    }
}
