package com.woocommerce.android.ui.orders

import androidx.navigation.NavController
import com.automattic.android.tracks.crashlogging.CrashLogging
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderNavigationLogger @Inject constructor(private val crashLogging: CrashLogging) {

    fun logCurrentGraph(navController: NavController, actionDescription: String) {
        val currentGraphId = navController.graph.id
        val currentGraphName = navController.graph.label ?: "Unknown"
        crashLogging.recordEvent("$actionDescription: Current graph ID=$currentGraphId, Name=$currentGraphName")
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
    }

    fun logGraphChangeIfNeeded(navController: NavController, previousGraphId: Int, actionDescription: String) {
        val newGraphId = navController.graph.id
        if (previousGraphId != newGraphId) {
            val newGraphName = navController.graph.label ?: "Unknown"
            crashLogging.recordEvent("$actionDescription: Graph changed to ID=$newGraphId, Name=$newGraphName")
        }
    }
}
