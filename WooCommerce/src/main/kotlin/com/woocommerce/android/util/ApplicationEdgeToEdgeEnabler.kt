package com.woocommerce.android.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import javax.inject.Inject

class ApplicationEdgeToEdgeEnabler @Inject constructor() : Application.ActivityLifecycleCallbacks {
    /**
     * Called when an activity has been created.
     *
     * All our activities extend [ComponentActivity], so one that doesn't comes from an SDK and handles its own
     * edge-to-edge. SDK activities that do extend [ComponentActivity] still get it enabled here.
     *
     * @param activity The activity that has been created.
     * @param bundle  A Bundle containing the activity's previously frozen state, if there was one.
     *                This parameter is not used within this method but is part of the lifecycle callback.
     */
    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        (activity as? ComponentActivity)?.enableEdgeToEdge()
    }

    override fun onActivityStarted(activity: Activity) {
        // no-op
    }

    override fun onActivityResumed(activity: Activity) {
        // no-op
    }

    override fun onActivityPaused(activity: Activity) {
        // no-op
    }

    override fun onActivityStopped(activity: Activity) {
        // no-op
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        // no-op
    }

    override fun onActivityDestroyed(activity: Activity) {
        // no-op
    }
}
