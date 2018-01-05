package com.woocommerce.android.ui.main

import android.support.test.espresso.Espresso
import android.support.test.espresso.NoActivityResumedException
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class AppNavigationTest {
    @Rule
    @JvmField var activityTestRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun pressingBackOnMainScreenExitsApp() {
        assertPressingBackExitsApp()
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
