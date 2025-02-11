package com.woocommerce.android.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.automattic.android.tracks.crashlogging.CrashLogging
import javax.inject.Inject

class ApplicationEdgeToEdgeEnabler @Inject constructor(
    private val crashLogger: CrashLogging
) : Application.ActivityLifecycleCallbacks {

    private val unsupportedEdgeToEdgeActivities = setOf(
        "leakcanary.internal.RequestPermissionActivity"
    )

    override fun onActivityCreated(activity: Activity, bindle: Bundle?) {
        val isEdgeToEdgeSupported = isEdgeToEdgeSupported(activity)
        if (PackageUtils.isDebugBuild() && isEdgeToEdgeSupported) {
            (activity as ComponentActivity).enableEdgeToEdge()
        } else {
            (activity as? ComponentActivity)?.enableEdgeToEdge() ?: run {
                if (isEdgeToEdgeSupported) {
                    val message = "Activity $activity is not a ComponentActivity"
                    crashLogger.sendReport(
                        exception = ClassCastException(message),
                        message = message
                    )
                }
            }
        }
    }

    private fun isEdgeToEdgeSupported(activity: Activity): Boolean {
        return (activity::class.java.name in unsupportedEdgeToEdgeActivities).not()
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
