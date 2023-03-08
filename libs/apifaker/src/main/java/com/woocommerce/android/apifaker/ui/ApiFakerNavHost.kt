package com.woocommerce.android.apifaker.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.woocommerce.android.apifaker.ui.details.EndpointDetailsScreen
import com.woocommerce.android.apifaker.ui.details.MISSING_ENDPOINT_ID
import com.woocommerce.android.apifaker.ui.home.HomeScreen

@Composable
fun ApiFakerNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route()
    ) {
        composable(Screen.Home.route()) {
            HomeScreen(hiltViewModel(), navController)
        }
        composable(
            Screen.EndpointDetails.routeTemplate,
            arguments = listOf(navArgument(Screen.EndpointDetails.endpointIdArgumentName) {
                type = NavType.LongType
                defaultValue = MISSING_ENDPOINT_ID
            })
        ) {
            EndpointDetailsScreen(hiltViewModel(), navController)
        }
    }
}
