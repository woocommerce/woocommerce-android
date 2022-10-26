package com.woocommerce.android.ui.appwidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import com.woocommerce.android.ui.appwidgets.WidgetUtils.WIDGET_NO_REGISTERED
import com.woocommerce.android.ui.appwidgets.stats.today.TodayStatsWidgetProvider
import com.woocommerce.android.ui.main.MainActivity
import java.util.Date
import kotlin.random.Random

/**
 * Utils class for for displaying data and handling click events in widgets.
 * Currently used to display stats widget list OR error message if stats is unavailable.
 */
object WidgetUtils {
    private const val PENDING_INTENT_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    const val WIDGET_NO_REGISTERED = "no-registered"

    fun getWidgetTapPendingIntent(
        context: Context,
        widgetName: String
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.FIELD_OPENED_FROM_WIDGET, true)
            putExtra(MainActivity.FIELD_WIDGET_NAME, widgetName)
        }
        return PendingIntent.getActivity(
            context,
            getRandomId(),
            intent,
            PENDING_INTENT_FLAGS
        )
    }

    fun getRetryIntent(
        context: Context,
        widgetType: Class<*>,
        appWidgetId: Int
    ): PendingIntent? {
        val intentSync = Intent(context, widgetType)
        intentSync.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

        intentSync.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        return PendingIntent.getBroadcast(
            context,
            Random(appWidgetId).nextInt(),
            intentSync,
            PENDING_INTENT_FLAGS
        )
    }

    private fun getRandomId(): Int {
        return Random(Date().time).nextInt()
    }
}

fun AppWidgetProviderInfo.getWidgetName(): String {
    return when (this.provider.className) {
        TodayStatsWidgetProvider::class.java.name -> TodayStatsWidgetProvider.WIDGET_NAME
        else -> WIDGET_NO_REGISTERED
    }
}
