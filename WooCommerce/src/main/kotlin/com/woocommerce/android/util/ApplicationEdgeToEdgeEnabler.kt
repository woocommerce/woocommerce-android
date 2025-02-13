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

    /**
     * Set of activity class names that should not be edge-to-edge.
     */
    private val unsupportedEdgeToEdgeActivities = setOf(
        "leakcanary.internal.RequestPermissionActivity"
    )

    /**
     * Called when an activity has been created.
     *
     * This function checks if edge-to-edge display is supported and, if so, enables it.
     * It handles different scenarios based on the debug/release build type and whether the
     * activity is a ComponentActivity.
     *
     * @param activity The activity that has been created.
     * @param bundle  A Bundle containing the activity's previously frozen state, if there was one.
     *                This parameter is not used within this method but is part of the lifecycle callback.
     *
     * @throws ClassCastException if `isEdgeToEdgeSupported` is true and the activity is not an instance of `ComponentActivity`,
     *                            a `ClassCastException` is thrown. The exception is caught and logged by the crashLogger.
     *
     * Behavior:
     *   - Checks if edge-to-edge is supported using `isEdgeToEdgeSupported(activity)`.
     *   - **Debug Build:**
     *     - If edge-to-edge is supported, it calls `enableEdgeToEdge()` directly on the activity,
     *       assuming it's a `ComponentActivity`.
     *   - **Release Build or Edge-to-Edge Not Supported in Debug build:**
     *     - If the activity is a `ComponentActivity`, it calls `enableEdgeToEdge()`.
     *     - If the activity is not a `ComponentActivity` but edge-to-edge is supported, it logs a
     *       `ClassCastException` using `crashLogger.sendReport()`.
     *     - If the activity is not a `ComponentActivity` and edge-to-edge isn't supported, nothing happens.
     *
     * Edge Case Handling:
     *  - In release builds, the code gracefully handles non-ComponentActivities when edge-to-edge is supported by logging the issue instead of crashing.
     *  - In debug builds, the code will attempt to cast the activity to a `ComponentActivity`. If the cast fails, then a `ClassCastException` will be thrown by the application.
     */
    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
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
