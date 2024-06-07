package com.woocommerce.android.ui.woopos.root.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

@Composable
fun WooPosRootHost(modifier: Modifier = Modifier) {
    val rootController = rememberNavController()

    NavHost(
        modifier = modifier,
        navController = rootController,
        startDestination = MAIN_GRAPH_ROUTE,
        enterTransition = { screenSlideIn() },
        exitTransition = { screenFadeOut() },
        popEnterTransition = { screenFadeIn() },
        popExitTransition = { screenSlideOut() },
    ) {
        checkoutGraph(navController = rootController)
    }
}
