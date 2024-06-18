package com.woocommerce.android.ui.woopos.root.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

@Composable
fun WooPosRootHost(
    modifier: Modifier = Modifier,
    rootController: NavHostController,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    NavHost(
        modifier = modifier,
        navController = rootController,
        startDestination = MAIN_GRAPH_ROUTE,
        enterTransition = { screenSlideIn() },
        exitTransition = { screenFadeOut() },
        popEnterTransition = { screenFadeIn() },
        popExitTransition = { screenSlideOut() },
    ) {
        mainGraph(onNavigationEvent = onNavigationEvent)
    }
}
