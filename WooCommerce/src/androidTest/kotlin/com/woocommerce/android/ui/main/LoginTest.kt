package com.woocommerce.android.ui.main

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.woocommerce.android.R
import com.woocommerce.android.R.id
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.anything
import org.hamcrest.Matchers
import org.hamcrest.core.AllOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LoginTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun validLoginTest() {

        // click login button on Welcome page
        onView(withText("Log in")).perform(click())

        // clear text on the Site address field
        onView(withId(R.id.input)).perform(clearText())

        // enter site address
        onView(withId(R.id.input)).perform(typeText("https://usual-frigate.jurassic.ninja"))

        onView(withText("Next")).perform(click())

        Thread.sleep(20000)

        onView(withId(R.id.input)).perform(typeText(""))

        Thread.sleep(3000)

        onView(withText("Next")).perform(click())

        Thread.sleep(3000)

        onView(withText("Enter your password instead")).perform(click())

        onView(withId(R.id.input)).perform(typeText(""), closeSoftKeyboard())

        Thread.sleep(5000)

        onView(withText("Next")).perform(click())

        onView(withText("Logging in")).check(matches(isDisplayed()))
        Thread.sleep(5000)

        onView(withText("Usual Frigate")).check(matches(isDisplayed()))
    }

    @Test
    fun invalidSiteAddressTest() {
        onView(withText("Log in")).perform(click())

        onView(withId(R.id.input)).perform(clearText())

        onView(withId(R.id.input)).perform(typeText("https://usual-fry.com"))

        onView(withText("Next")).perform(click())

        Thread.sleep(4000)

        onView(withId(R.id.textinput_error)).check(matches(withText("Check that the site URL entered is valid")))
    }

    @Test
    fun invalidSiteCreds() {

        onView(withText("Log in")).perform(click())

        onView(withId(id.input)).perform(clearText())

        onView(withId(id.input)).perform(typeText("https://usual-frigate.jurassic.ninja"))

        onView(withText("Next")).perform(click())

        Thread.sleep(20000)

        onView(withId(id.login_site_button)).perform(click())


        Thread . sleep (3000)

        onView(withContentDescription("Username")).perform(typeText("adcd@abcd.com"))

        onView(AllOf.allOf(withId(R.id.input), withText("Password"),withId(R.id.input_layout))).perform(typeText("adcd"))

        onView(withText("Next")).perform(click())

        Thread . sleep (10000)

        onView (withId(id.textinput_error)).check(matches(withText("Please enter a username")))
    }
}
