package com.woocommerce.android.ui.main

import android.support.design.widget.BottomNavigationView
import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.NoActivityResumedException
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.filters.LargeTest
import android.support.test.runner.AndroidJUnit4
import com.woocommerce.android.R
import com.woocommerce.android.ui.TestBase
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.SiteModel
import com.woocommerce.android.helpers.WCMatchers
import org.hamcrest.Matchers.equalToIgnoringCase
import org.junit.Before

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

    @Test
    fun pressingBackAfterBottomNavOptionChangeExitsApp() {
        // Verify switching bottom bar tabs does not retain a back stack.
        // Switch from the default dashboard tab to the notifications tab
        onView(withId(R.id.notifications)).perform(click())

        // Clicking back should not switch back to the previous tab, it should
        // exit the app.
        assertPressingBackExitsApp()
    }

    @Test
    fun appDisplaysDashboardOnLaunch() {
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
        // Select the orders bottom menu option
        onView(withId(R.id.orders)).perform(click())

        // Verify the toolbar title has changed to Orders
        onView(withId(R.id.toolbar)).check(matches(
                WCMatchers.withToolbarTitle(equalToIgnoringCase(appContext.getString(R.string.orders)))))
    }

    @Test
    fun notificationsMenuOptionDisplaysNotificationsView() {
        // Select the notifications bottom bar option
        onView(withId(R.id.notifications)).perform(click())

        // Verify the toolbar title has changed to Notifications
        onView(withId(R.id.toolbar)).check(matches(
                WCMatchers.withToolbarTitle(equalToIgnoringCase(appContext.getString(R.string.notifications)))))
    }

    @Test
    fun dashboardMenuOptionDisplaysDashboardView() {
        // Switch away from the default selected dashboard option
        onView(withId(R.id.notifications)).perform(click())

        // Select the dashboard bottom bar option
        onView(withId(R.id.dashboard)).perform(click())

        // Verify the toolbar title has changed to 'My Store'
        onView(withId(R.id.toolbar)).check(matches(
                WCMatchers.withToolbarTitle(equalToIgnoringCase(appContext.getString(R.string.my_store)))))
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
