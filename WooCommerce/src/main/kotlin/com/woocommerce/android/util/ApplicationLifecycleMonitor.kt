package com.woocommerce.android.util

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.os.Bundle

class ApplicationLifecycleMonitor(private val lifecycleListener: ApplicationLifecycleListener)
    : Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {
    interface ApplicationLifecycleListener {
        fun onAppComesFromBackground()
        fun onFirstActivityResumed()
        fun onAppGoesToBackground()
    }

    private enum class LastApplicationState {
        BACKGROUND, FOREGROUND
    }

    private var lastState = LastApplicationState.BACKGROUND
    private var firstActivityResumed = true

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {
        if (lastState == LastApplicationState.BACKGROUND) {
            lastState = LastApplicationState.FOREGROUND
            lifecycleListener.onAppComesFromBackground()
        }

        if (firstActivityResumed) {
            firstActivityResumed = false
            lifecycleListener.onFirstActivityResumed()
        }
    }

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityDestroyed(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivityCreated(activity: Activity, p1: Bundle?) {}

    override fun onLowMemory() {}

    override fun onConfigurationChanged(configuration: Configuration) {}

    override fun onTrimMemory(level: Int) {
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            lastState = LastApplicationState.BACKGROUND
            lifecycleListener.onAppGoesToBackground()
        }
    }
}
