package com.woocommerce.android.ui.main

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.window.OnBackInvokedDispatcher
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_CONTEXT
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class BackPressTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    // A consumed callback can enqueue a pop, so both tracking reports arrive before the main queue advances.
    private var trackedInCurrentMainLoop = false
    private var suppressNextTrack = false

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

    @MainThread
    internal fun trackBackPressed(view: Any) {
        if (suppressNextTrack) {
            suppressNextTrack = false
            return
        }
        if (trackedInCurrentMainLoop) return

        trackedInCurrentMainLoop = true
        mainHandler.post {
            trackedInCurrentMainLoop = false
        }
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.BACK_PRESSED,
            properties = mapOf(KEY_CONTEXT to view::class.java.simpleName)
        )
    }

    @MainThread
    internal fun armNextTrackSuppression() {
        suppressNextTrack = true
    }

    private fun backStackChangedListener(view: Any) = object : FragmentManager.OnBackStackChangedListener {
        private var popCommitted = false
        private var suppressCurrentPop = false

        override fun onBackStackChangeCommitted(fragment: Fragment, pop: Boolean) {
            if (!pop) return

            popCommitted = true
            if (
                fragment.isRemoving &&
                (fragment as? BackResolutionOwner)?.consumePendingBackResolution() == true
            ) {
                suppressCurrentPop = true
            }
        }

        override fun onBackStackChanged() {
            if (!popCommitted) {
                suppressNextTrack = false
                return
            }

            val shouldSuppress = suppressCurrentPop
            resetCurrentPop()
            if (shouldSuppress) {
                suppressNextTrack = false
            } else {
                trackBackPressed(view)
            }
        }

        override fun onBackStackChangeCancelled() {
            resetCurrentPop()
            suppressNextTrack = false
        }

        private fun resetCurrentPop() {
            popCommitted = false
            suppressCurrentPop = false
        }
    }
}

interface BackResolutionOwner {
    @MainThread
    fun consumePendingBackResolution(): Boolean
}

interface BackPressTrackerOwner {
    val backPressTracker: BackPressTracker
}
