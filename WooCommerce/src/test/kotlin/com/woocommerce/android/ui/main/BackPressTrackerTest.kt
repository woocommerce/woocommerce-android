package com.woocommerce.android.ui.main

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
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class BackPressTrackerTest {
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private lateinit var activity: FragmentActivity
    private lateinit var fragmentManager: FragmentManager

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        fragmentManager = activity.supportFragmentManager
        fragmentManager.beginTransaction()
            .add(Fragment(), INITIAL_FRAGMENT_TAG)
            .commitNow()
        BackPressTracker(analyticsTrackerWrapper).register(activity, fragmentManager)
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
