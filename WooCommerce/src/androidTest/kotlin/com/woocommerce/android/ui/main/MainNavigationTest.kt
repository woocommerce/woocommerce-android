package com.woocommerce.android.ui.main

import android.widget.ListView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.NoActivityResumedException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.woocommerce.android.R
import com.woocommerce.android.helpers.WCMatchers
import com.woocommerce.android.ui.TestBase
import org.hamcrest.Matchers.equalToIgnoringCase
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.SiteModel

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainNavigationTest : TestBase() {
    @Rule
    @JvmField var activityTestRule = MainActivityTestRule()

    @Before
    override fun setup() {
        super.setup()
        activityTestRule.launchMainActivityLoggedIn(null, SiteModel())
    }

    @Test
    fun pressingBackOnMainScreenExitsApp() {
        assertPressingBackExitsApp()
    }

    // This test is disabled, see https://github.com/woocommerce/woocommerce-android/issues/2634
    @Ignore
    @Test
    fun pressingBackAfterBottomNavOptionChangeExitsApp() {
        // Make sure the bottom navigation view is showing
        activityTestRule.activity.showBottomNav()

        // add mock data to order list screen
        activityTestRule.setOrderListWithMockData()

        // Verify switching bottom bar tabs does not retain a back stack.
        // Switch from the default dashboard tab to the orders tab
        onView(withId(R.id.orders)).perform(click())

        // Clicking back should not switch back to the previous tab, it should
        // exit the app.
        assertPressingBackExitsApp()
    }

    @Test
    fun appDisplaysDashboardOnLaunch() {
        // Make sure the bottom navigation view is showing
        activityTestRule.activity.showBottomNav()

        // The Dashboard tab should be active if the app is launched
        onView(withId(R.id.bottom_nav)).check { view, noMatchException ->
            view?.let {
                val selectedMenuItem = (it as BottomNavigationView).selectedItemId
                if (selectedMenuItem != R.id.dashboard) throw AssertionError("Dashboard is not selected!")
            } ?: throw noMatchException
        }

        // Verify the toolbar title is 'My Store'
        onView(withId(R.id.toolbar)).check(matches(
                WCMatchers.withToolbarTitle(equalToIgnoringCase(appContext.getString(R.string.my_store)))))
    }

    @Test
    fun ordersMenuOptionDisplaysOrdersView() {
        // Make sure the bottom navigation view is showing
        activityTestRule.activity.showBottomNav()

        // add mock data to order list screen
        activityTestRule.setOrderListWithMockData()

        // Select the orders bottom menu option
        onView(withId(R.id.orders)).perform(click())

        // Verify the toolbar title has changed to Orders
        onView(withId(R.id.toolbar)).check(matches(
                WCMatchers.withToolbarTitle(equalToIgnoringCase(appContext.getString(R.string.orders)))))
    }

    @Test
    fun reviewsMenuOptionDisplaysReviewsView() {
        // Make sure the bottom navigation view is showing
        activityTestRule.activity.showBottomNav()

        // add mock data to the reviews list screen
        activityTestRule.setReviewListWithMockData()

        // Select the reviews bottom bar option
        onView(withId(R.id.reviews)).perform(click())

        // Verify the toolbar title has changed to 'Reviews'
        onView(withId(R.id.toolbar)).check(matches(
                WCMatchers.withToolbarTitle(equalToIgnoringCase(
                        appContext.getString(R.string.product_reviews)))))
    }

    @Test
    fun dashboardMenuOptionDisplaysDashboardView() {
        // Make sure the bottom navigation view is showing
        activityTestRule.activity.showBottomNav()

        // add mock data to order list screen
        activityTestRule.setOrderListWithMockData()

        // Switch away from the default selected dashboard option
        onView(withId(R.id.orders)).perform(click())

        // Select the dashboard bottom bar option
        onView(withId(R.id.dashboard)).perform(click())

        // Verify the toolbar title has changed to 'My Store'
        onView(withId(R.id.toolbar)).check(matches(
                WCMatchers.withToolbarTitle(equalToIgnoringCase(appContext.getString(R.string.my_store)))))
    }

    @Test
    fun verifyOverFlowMenuAndSettingsIsDisplayed() {
        // Open the overflow menu OR open the options menu,
        // depending on if the device has a hardware or software overflow menu button.
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)

        // verify that there are 2 items in the menu list
        onView(isAssignableFrom(ListView::class.java))
                .check(matches(WCMatchers.withItemCount(2)))

        // verify that the menu is displayed & the first item in the list is Settings
        onView(withText(appContext.getString(R.string.settings))).check(matches(isDisplayed()))

        // verify that when first item is clicked, it opens settings
        // by checking if the toolbar title is changed to Settings
        // by checking if the UP indicator is visible
        onView(withText(appContext.getString(R.string.settings))).perform(click())
        onView(withId(R.id.toolbar)).check(matches(
                WCMatchers.withToolbarTitle(equalToIgnoringCase(appContext.getString(R.string.settings)))))
        onView(withContentDescription(R.string.abc_action_bar_up_description)).check(matches(isDisplayed()))

        // click back button and verify that the toolbar title is changed
        // up button is no longer visible
        onView(withContentDescription(R.string.abc_action_bar_up_description)).perform(click())
        onView(withId(R.id.toolbar)).check(matches(
                WCMatchers.withToolbarTitle(equalToIgnoringCase(appContext.getString(R.string.my_store)))))
        onView(withContentDescription(
                appContext.getString(R.string.abc_action_bar_up_description))
        ).check(doesNotExist())
    }

    @Test
    fun verifyOverFlowMenuAndHelpIsDisplayed() {
        // Open the overflow menu OR open the options menu,
        // depending on if the device has a hardware or software overflow menu button.
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)

        // verify that there are 2 items in the menu list
        onView(isAssignableFrom(ListView::class.java))
                .check(matches(WCMatchers.withItemCount(2)))

        // verify that the menu is displayed & the second item in the list is Help
        onView(withText(appContext.getString(R.string.support_help))).check(matches(isDisplayed()))

        // verify that when first item is clicked, it opens Help & Support
        // by checking if the toolbar title is changed to Help & Support
        // by checking if the UP indicator is visible
        onView(withText(appContext.getString(R.string.support_help))).perform(click())
        onView(withId(R.id.toolbar)).check(matches(
                WCMatchers.withToolbarTitle(equalToIgnoringCase(appContext.getString(R.string.support_help)))))
        onView(withContentDescription(R.string.abc_action_bar_up_description)).check(matches(isDisplayed()))

        // click back button and verify that the toolbar title is changed
        // up button is no longer visible
        onView(withContentDescription(R.string.abc_action_bar_up_description)).perform(click())
        onView(withId(R.id.toolbar)).check(matches(
                WCMatchers.withToolbarTitle(equalToIgnoringCase(appContext.getString(R.string.my_store)))))
        onView(withContentDescription(
                appContext.getString(R.string.abc_action_bar_up_description))
        ).check(doesNotExist())
    }

    private fun assertPressingBackExitsApp() {
        try {
            Espresso.pressBack()
            fail("Expected app to be closed and throw an exception")
        } catch (e: NoActivityResumedException) {
            // Test OK
        }
    }
}
