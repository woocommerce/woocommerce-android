package com.woocommerce.android.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_CONTEXT
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.main.BackPressTracker
import com.woocommerce.android.ui.main.BackPressTrackerOwner
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BaseFragmentTest {
    @Test
    fun `given listener consumes back, when back is pressed, then host callback is not invoked`() {
        // GIVEN
        val setup = givenBackPressSetup(allowBackPress = false)

        // WHEN
        setup.activity.onBackPressedDispatcher.onBackPressed()

        // THEN
        assertThat(setup.fragment.backPressCalls).isEqualTo(1)
        assertThat(setup.hostBackPressCalls()).isZero()
        verify(setup.analyticsTrackerWrapper).track(
            stat = AnalyticsEvent.BACK_PRESSED,
            properties = mapOf(KEY_CONTEXT to BackPressTestActivity::class.java.simpleName)
        )
    }

    @Test
    fun `given listener allows back, when back is pressed, then host callback is invoked once`() {
        // GIVEN
        val setup = givenBackPressSetup(allowBackPress = true)

        // WHEN
        setup.activity.onBackPressedDispatcher.onBackPressed()

        // THEN
        assertThat(setup.fragment.backPressCalls).isEqualTo(1)
        assertThat(setup.hostBackPressCalls()).isEqualTo(1)
    }

    @Test
    fun `given listener consumes back, when navigation continues later, then listener is not invoked again`() {
        // GIVEN
        val setup = givenBackPressSetup(allowBackPress = false)
        setup.activity.onBackPressedDispatcher.onBackPressed()

        // WHEN
        setup.fragment.continueBackNavigation()

        // THEN
        assertThat(setup.fragment.backPressCalls).isEqualTo(1)
        assertThat(setup.hostBackPressCalls()).isEqualTo(1)
    }

    private fun givenBackPressSetup(allowBackPress: Boolean): BackPressSetup {
        val activity = Robolectric.buildActivity(BackPressTestActivity::class.java).setup().get()
        val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
        activity.backPressTracker = BackPressTracker(analyticsTrackerWrapper)
        var hostBackPressCalls = 0
        activity.onBackPressedDispatcher.addCallback(
            activity,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    hostBackPressCalls++
                }
            }
        )
        val fragment = TestBackPressFragment().apply {
            this.allowBackPress = allowBackPress
        }
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()
        return BackPressSetup(activity, fragment, analyticsTrackerWrapper) { hostBackPressCalls }
    }

    class BackPressTestActivity : FragmentActivity(), BackPressTrackerOwner {
        override lateinit var backPressTracker: BackPressTracker
    }

    class TestBackPressFragment : BaseFragment(), BackPressListener {
        var allowBackPress = false
        var backPressCalls = 0
            private set

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View = View(requireContext())

        override fun onRequestAllowBackPress(): Boolean {
            backPressCalls++
            return allowBackPress
        }
    }

    private data class BackPressSetup(
        val activity: BackPressTestActivity,
        val fragment: TestBackPressFragment,
        val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
        val hostBackPressCalls: () -> Int
    )
}
