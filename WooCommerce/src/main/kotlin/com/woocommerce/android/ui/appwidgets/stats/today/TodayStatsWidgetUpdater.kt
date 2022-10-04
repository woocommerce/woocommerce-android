package com.woocommerce.android.ui.appwidgets.stats.today

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.woocommerce.android.ui.appwidgets.WidgetUpdater
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TodayStatsWidgetUpdater @Inject constructor() : WidgetUpdater {
    override fun updateAppWidget(context: Context, appWidgetId: Int, appWidgetManager: AppWidgetManager?) {
        val uniqueName = getUniqueName(appWidgetId)
        val data = workDataOf(UpdateTodayStatsWorker.DATA_WIDGET_ID to appWidgetId)

        val workRequest =
            PeriodicWorkRequestBuilder<UpdateTodayStatsWorker>(
                TodayStatsWidgetProvider.WIDGET_UPDATE_INTERVAL,
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

    override fun componentName(context: Context) = ComponentName(context, TodayStatsWidgetProvider::class.java)

    override fun delete(context: Context, appWidgetId: Int) {
        val uniqueName = getUniqueName(appWidgetId)
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName)
    }

    private fun getUniqueName(appWidgetId: Int): String = "$appWidgetId - ${TodayStatsWidgetProvider.WIDGET_NAME}"
}
