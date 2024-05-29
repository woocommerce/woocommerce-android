package com.woocommerce.android.ui.woopos.root.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

@Composable
fun WooPosRootHost(
    connectToCardReader: (context: Context) -> Unit,
) {
    val rootController = rememberNavController()

    NavHost(
        navController = rootController,
        startDestination = MAIN_GRAPH_ROUTE,
        enterTransition = { screenSlideIn() },
        exitTransition = { screenFadeOut() },
        popEnterTransition = { screenFadeIn() },
        popExitTransition = { screenSlideOut() },
    ) {
        checkoutGraph(
            navController = rootController,
            onConnectToCardReaderClicked = { context -> connectToCardReader(context) },
        )
    }
}
