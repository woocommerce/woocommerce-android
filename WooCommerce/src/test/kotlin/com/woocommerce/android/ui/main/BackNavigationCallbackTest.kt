package com.woocommerce.android.ui.main

import androidx.activity.addCallback
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BackNavigationCallbackTest {
    @Test
    fun `given host callback is registered, when back is pressed, then activity handles before host`() {
        // GIVEN
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val navHostFragment = NavHostFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(navHostFragment, NAV_HOST_TAG)
            .commitNow()
        val calls = mutableListOf<String>()
        activity.onBackPressedDispatcher.addCallback(activity) {
            calls += HOST_CALLBACK
        }
        activity.addBackNavigationCallbackAfterNavHost(navHostFragment) {
            calls += ACTIVITY_CALLBACK
        }

        // WHEN
        activity.onBackPressedDispatcher.onBackPressed()

        // THEN
        assertThat(calls).containsExactly(ACTIVITY_CALLBACK, HOST_CALLBACK)
    }

    @Test
    fun `given nav host is not attached, when activity callback is registered, then registration fails`() {
        // GIVEN
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val navHostFragment = NavHostFragment()

        // WHEN
        val registration = {
            activity.addBackNavigationCallbackAfterNavHost(navHostFragment) {}
        }

        // THEN
        assertThatIllegalStateException().isThrownBy(registration)
    }

    private companion object {
        const val NAV_HOST_TAG = "navHost"
        const val ACTIVITY_CALLBACK = "activity"
        const val HOST_CALLBACK = "host"
    }
}
