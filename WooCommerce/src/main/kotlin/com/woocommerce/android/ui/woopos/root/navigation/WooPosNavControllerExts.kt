package com.woocommerce.android.ui.woopos.root.navigation

import androidx.navigation.NavController

fun NavController.navigateOnce(route: String) {
    if (currentRouteWithParams() != route) {
        navigate(route)
    }
}

private fun NavController.currentRouteWithParams(): String {
    val currentRoute = currentDestination?.route ?: return ""
    val arguments = currentBackStackEntry?.arguments ?: return currentRoute

    return currentRoute.split("/").joinToString("/") { segment ->
        if (segment.startsWith("{") && segment.endsWith("}")) {
            val key = segment.removeSurrounding("{", "}")
            @Suppress("DEPRECATION")
            arguments.get(key)?.toString() ?: segment
        } else {
            segment
        }
    }
}
