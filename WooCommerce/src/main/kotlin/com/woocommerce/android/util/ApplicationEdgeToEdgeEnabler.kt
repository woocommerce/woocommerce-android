package com.woocommerce.android.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import javax.inject.Inject

class ApplicationEdgeToEdgeEnabler @Inject constructor() : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, bindle: Bundle?) {
        (activity as ComponentActivity).enableEdgeToEdge()
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }
}
