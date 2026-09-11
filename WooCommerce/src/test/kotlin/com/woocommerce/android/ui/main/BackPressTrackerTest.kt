package com.woocommerce.android.ui.main

import android.os.Looper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_CONTEXT
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class BackPressTrackerTest {
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private lateinit var activity: FragmentActivity
    private lateinit var backPressTracker: BackPressTracker
    private lateinit var fragmentManager: FragmentManager

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        fragmentManager = activity.supportFragmentManager
        fragmentManager.beginTransaction()
            .add(Fragment(), INITIAL_FRAGMENT_TAG)
            .commitNow()
        backPressTracker = BackPressTracker(analyticsTrackerWrapper)
        backPressTracker.register(activity, fragmentManager)
    }

    @Test
    fun `when a back stack entry is popped, then back press is tracked exactly once`() {
        // GIVEN
        givenForwardNavigation()

        // WHEN
        fragmentManager.popBackStackImmediate()

        // THEN
        verify(analyticsTrackerWrapper).track(
            stat = AnalyticsEvent.BACK_PRESSED,
            properties = mapOf(KEY_CONTEXT to FragmentActivity::class.java.simpleName)
        )
    }

    @Test
    fun `when navigating forward, then back press is not tracked`() {
        // WHEN
        givenForwardNavigation()

        // THEN
        verifyNoInteractions(analyticsTrackerWrapper)
    }

    @Test
    fun `given consumed back is tracked, when it also pops, then back press is tracked exactly once`() {
        // GIVEN
        givenForwardNavigation()
        backPressTracker.trackBackPressed(activity)

        // WHEN
        fragmentManager.popBackStackImmediate()

        // THEN
        verify(analyticsTrackerWrapper).track(
            stat = AnalyticsEvent.BACK_PRESSED,
            properties = mapOf(KEY_CONTEXT to FragmentActivity::class.java.simpleName)
        )
    }

    @Test
    fun `given previous tracking completed, when back is pressed again, then both presses are tracked`() {
        // GIVEN
        backPressTracker.trackBackPressed(activity)
        shadowOf(Looper.getMainLooper()).idle()

        // WHEN
        backPressTracker.trackBackPressed(activity)

        // THEN
        verify(analyticsTrackerWrapper, times(2)).track(
            stat = AnalyticsEvent.BACK_PRESSED,
            properties = mapOf(KEY_CONTEXT to FragmentActivity::class.java.simpleName)
        )
    }

    private fun givenForwardNavigation() {
        val initialFragment = checkNotNull(fragmentManager.findFragmentByTag(INITIAL_FRAGMENT_TAG))
        fragmentManager.beginTransaction()
            .remove(initialFragment)
            .add(Fragment(), NEXT_FRAGMENT_TAG)
            .addToBackStack(null)
            .commit()
        fragmentManager.executePendingTransactions()
    }

    private companion object {
        const val INITIAL_FRAGMENT_TAG = "initial"
        const val NEXT_FRAGMENT_TAG = "next"
    }
}
