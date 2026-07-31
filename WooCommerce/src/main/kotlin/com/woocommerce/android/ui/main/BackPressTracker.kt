package com.woocommerce.android.ui.main

import android.app.Activity
import android.os.Build
import android.window.OnBackInvokedDispatcher
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_CONTEXT
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class BackPressTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) {
    fun register(activity: Activity, fragmentManager: FragmentManager) {
        fragmentManager.addOnBackStackChangedListener(backStackChangedListener(activity))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            activity.onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_SYSTEM_NAVIGATION_OBSERVER
            ) {
                trackBackPressed(activity)
            }
        }
    }

    internal fun trackBackPressed(view: Any) {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.BACK_PRESSED,
            properties = mapOf(KEY_CONTEXT to view::class.java.simpleName)
        )
    }

    private fun backStackChangedListener(view: Any) = object : FragmentManager.OnBackStackChangedListener {
        private var trackedCurrentPop = false

        override fun onBackStackChangeCommitted(fragment: Fragment, pop: Boolean) {
            if (pop && !trackedCurrentPop) {
                trackBackPressed(view)
                trackedCurrentPop = true
            }
        }

        override fun onBackStackChanged() {
            trackedCurrentPop = false
        }
    }
}
