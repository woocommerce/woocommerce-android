package com.woocommerce.android.screenshots.util.screenhelpers

import android.app.Activity
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage.RESUMED

class ActivityHelper {
    companion object {
        private var mCurrentActivity: Activity? = null
        fun getCurrentActivity(): Activity? {
            InstrumentationRegistry.getInstrumentation()
                .runOnMainSync {
                    val resumedActivities: Collection<*> = ActivityLifecycleMonitorRegistry
                        .getInstance()
                        .getActivitiesInStage(RESUMED)
                    mCurrentActivity = if (resumedActivities.iterator().hasNext()) {
                        resumedActivities.iterator().next() as Activity?
                    } else {
                        resumedActivities.toTypedArray()[0] as Activity?
                    }
                }
            return mCurrentActivity
        }

        fun getTranslatedString(resourceID: Int): String {
            return getCurrentActivity()!!.resources.getString(resourceID)
        }
    }
}
