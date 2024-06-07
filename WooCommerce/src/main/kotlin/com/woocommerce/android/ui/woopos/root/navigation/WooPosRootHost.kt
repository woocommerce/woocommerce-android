package com.woocommerce.android.ui.woopos.root.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

@Composable
fun WooPosRootHost(modifier: Modifier = Modifier) {
    val rootController = rememberNavController()
    val activity = LocalContext.current as ComponentActivity

    NavHost(
        modifier = modifier,
        navController = rootController,
        startDestination = MAIN_GRAPH_ROUTE,
        enterTransition = { screenSlideIn() },
        exitTransition = { screenFadeOut() },
        popEnterTransition = { screenFadeIn() },
        popExitTransition = { screenSlideOut() },
    ) {
        mainGraph(
            onNavigationEvent = { event ->
                rootController.handleNavigationEvent(event, activity)
            }
        )
    }
}

private fun NavHostController.handleNavigationEvent(
    event: WooPosNavigationEvent,
    activity: ComponentActivity
) {
    when (event) {
        is WooPosNavigationEvent.ExitPosClicked -> activity.finish()
    }
}
