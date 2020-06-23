package com.woocommerce.android.ui.demo;

import android.app.Activity;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import java.util.Collection;
import java.util.Iterator;

public class TestUtils {
  /**
   * Activity launcher.
   */
  public static Activity getCurrentActivity() {
    final Activity[] currentActivity = {null};
    InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
      public void run() {
        Collection<Activity> resumedActivity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(
                Stage.RESUMED);
        Iterator<Activity> it = resumedActivity.iterator();
        currentActivity[0] = it.next();
      }
    });

    return currentActivity[0];
  }

 // add screenshot func here
}
