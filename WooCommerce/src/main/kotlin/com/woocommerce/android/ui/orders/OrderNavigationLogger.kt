package com.woocommerce.android.ui.orders

import androidx.navigation.NavController
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.util.WooLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderNavigationLogger @Inject constructor(private val crashLogging: CrashLogging) {

    fun logCurrentGraph(navController: NavController, actionDescription: String) {
        val currentGraphId = navController.graph.id
        val currentGraphName = navController.graph.label ?: "Unknown"
        val message = "$actionDescription: Current graph ID=$currentGraphId, Name=$currentGraphName"
        crashLogging.recordEvent(message)
        WooLog.i(WooLog.T.ORDERS, message)
    }

    fun logBackStack(navController: NavController, actionDescription: String) {
        val currentEntry = navController.currentBackStackEntry
        val previousEntry = navController.previousBackStackEntry
        val currentEntryDesc = currentEntry?.destination?.label?.toString() ?: currentEntry?.destination?.id.toString()
        val previousEntryDesc =
            previousEntry?.destination?.label?.toString() ?: previousEntry?.destination?.id.toString()
        val logMessage = buildString {
            append("$actionDescription: ")
            append("Current Entry=$currentEntryDesc, ")
            append("Previous Entry=$previousEntryDesc")
        }
        crashLogging.recordEvent(logMessage)
        WooLog.i(WooLog.T.ORDERS, logMessage)
    }
}
