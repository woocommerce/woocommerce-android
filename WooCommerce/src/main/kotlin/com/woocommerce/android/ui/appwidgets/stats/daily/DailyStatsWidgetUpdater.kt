package com.woocommerce.android.ui.appwidgets.stats.daily

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.woocommerce.android.ui.appwidgets.WidgetUpdater
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DailyStatsWidgetUpdater @Inject constructor() : WidgetUpdater {
    override fun updateAppWidget(context: Context, appWidgetId: Int, appWidgetManager: AppWidgetManager?) {
        val uniqueName = getUniqueName(appWidgetId)
        val data = Data.Builder()
            .putInt(UpdateTodayStatsWorker.WIDGET_ID, appWidgetId)
            .build()

        val workRequest =
            PeriodicWorkRequestBuilder<UpdateTodayStatsWorker>(
                DailyStatsWidgetProvider.WIDGET_UPDATE_INTERVAL,
                TimeUnit.MINUTES
            )
                .setInputData(data)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(uniqueName, ExistingPeriodicWorkPolicy.REPLACE, workRequest)
    }

    override fun componentName(context: Context) = ComponentName(context, DailyStatsWidgetProvider::class.java)

    override fun delete(context: Context, appWidgetId: Int) {
        val uniqueName = getUniqueName(appWidgetId)
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName)
    }

    private fun getUniqueName(appWidgetId: Int): String = "$appWidgetId - ${DailyStatsWidgetProvider.WIDGET_NAME}"
}
