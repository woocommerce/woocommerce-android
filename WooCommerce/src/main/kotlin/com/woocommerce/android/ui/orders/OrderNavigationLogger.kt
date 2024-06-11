package com.woocommerce.android.ui.orders

import android.annotation.SuppressLint
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderNavigationLogger @Inject constructor() {

    @SuppressLint("RestrictedApi")
    @Suppress("TooGenericExceptionCaught")
    fun logBackStack(navController: NavController, actionDescription: String) {
        try {
            val backStackEntries: StateFlow<List<NavBackStackEntry>> = navController.currentBackStack
            val backStackDescriptions = backStackEntries.value.joinToString(separator = "; ") { entry ->
                val destination = entry.destination
                val id = destination.id
                val label = destination.label ?: "No label"
                val className = destination.javaClass.simpleName
                "ID=$id, Label=$label, Class=$className"
            }

            val rootGraph = navController.graph as? NavGraph
            val startDestination = rootGraph?.findNode(rootGraph.startDestinationId)
            val startDestinationDetails = startDestination?.let { destination ->
                val id = destination.id
                val label = destination.label ?: "No label"
                val className = destination.javaClass.simpleName
                "ID=$id, Label=$label, Class=$className"
            } ?: "No start destination"

            val logMessage = "$actionDescription: NavGraph=${rootGraph?.displayName}, " +
                "StartDestination={$startDestinationDetails}, " +
                "BackStackEntries=[$backStackDescriptions]"

            WooLog.d(WooLog.T.ORDERS, logMessage)
        } catch (exception: Exception) {
            WooLog.e(WooLog.T.ORDERS, exception)
        }
    }
}
