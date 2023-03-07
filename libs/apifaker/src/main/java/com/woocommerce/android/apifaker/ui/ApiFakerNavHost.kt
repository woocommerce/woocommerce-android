package com.woocommerce.android.apifaker.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.woocommerce.android.apifaker.ui.Screen.EndpointDetails
import com.woocommerce.android.apifaker.ui.Screen.Home
import com.woocommerce.android.apifaker.ui.home.HomeScreen

@Composable
fun ApiFakerNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Home.route()
    ) {
        composable(Home.route()) {
            HomeScreen(hiltViewModel(), navController)
        }
        composable(
            EndpointDetails.baseRoute,
            arguments = listOf(navArgument("endpointId") {
                type = NavType.IntType
                defaultValue = -1
            })
        ) {
            TODO()
        }
    }
}
