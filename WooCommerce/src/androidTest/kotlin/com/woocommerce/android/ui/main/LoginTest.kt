package com.woocommerce.android.ui.main

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.R.id
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.woocommerce.android.screenshots.login.WelcomeScreen
import com.woocommerce.android.screenshots.login.SiteAddressScreen
import com.woocommerce.android.screenshots.login.EmailAddressScreen
import com.woocommerce.android.screenshots.login.MagicLinkScreen
import com.woocommerce.android.screenshots.login.PasswordScreen
import com.woocommerce.android.screenshots.mystore.MyStoreScreen

@LargeTest
@RunWith(AndroidJUnit4::class)
class LoginTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun validLoginTest() {


        // click Log in button on Welcome Screen
        onView(withId(WelcomeScreen.LOGIN_BUTTON)).perform(click())

        // enter Site address screen
        onView(withId(SiteAddressScreen.SITE_ADDRESS_FIELD)).perform(typeText(BuildConfig.BASE_URL))

        // click Next button
        onView(withId(SiteAddressScreen.NEXT_BUTTON)).perform(click())

        // wait for checking site validation
        Thread.sleep(5000)

        // enter email address
        onView(withId(EmailAddressScreen.EMAIL_ADDRESS_FIELD)).perform(typeText(BuildConfig.EMAIL))

        // click Next button
        onView(withId(EmailAddressScreen.NEXT_BUTTON)).perform(click())

        // wait for Magic link screen
        Thread.sleep(3000)

        // click on the use password instead link
        onView(withId(MagicLinkScreen.USE_PASSWORD_BUTTON)).perform(click())

        // enter Password
        onView(withId(PasswordScreen.PASSWORD_FIELD)).perform(typeText(BuildConfig.PASSWORD))

        // click Next button
        onView(withId(PasswordScreen.NEXT_BUTTON)).perform(click())

        // Assert on the display of Logging in text
        onView(withText("Logging in")).check(matches(isDisplayed()))

        // assert that we are on My store screen
        onView(withId(MyStoreScreen.SETTINGS_BUTTON_TEXT)).check(matches(isDisplayed()))
    }

    @Test
    fun invalidSiteAddressTest() {

        //onView(withText("Log in")).perform(click())
        onView(withId(WelcomeScreen.LOGIN_BUTTON)).perform(click())

        // enter Site address screen
        onView(withId(SiteAddressScreen.SITE_ADDRESS_FIELD)).perform(typeText(BuildConfig.BAD_BASE_URL))

        // click Next button
        onView(withId(SiteAddressScreen.NEXT_BUTTON)).perform(click())

        // assert the validation error message
        onView(withId(SiteAddressScreen.INPUT_ERROR)).check(matches(withText(SiteAddressScreen.MESSAGE)))
    }

    @Test
    fun emptyEmailSiteCredentialsTest() {

        // click Log in button on Welcome Screen
        onView(withId(WelcomeScreen.LOGIN_BUTTON)).perform(click())

        // enter Site address screen
        onView(withId(SiteAddressScreen.SITE_ADDRESS_FIELD)).perform(typeText(BuildConfig.BASE_URL))

        // click Next button
        onView(withId(SiteAddressScreen.NEXT_BUTTON)).perform(click())

        // wait for checking site validation
        Thread.sleep(5000)

        // click on Login with Site credentials link
        onView(withId(MagicLinkScreen.LOGIN_WITH_SITE_CREDENTIALS_BUTTON)).perform(click())

        // enter Next button without entering email or password
        onView(withId(PasswordScreen.NEXT_BUTTON)).perform(click())

        // assert on the validation error message for empty email address
        onView (withId(id.textinput_error)).check(matches(withText(PasswordScreen.EMAIL_INPUT_VALIDATION)))
    }
}
