package com.woocommerce.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.woocommerce.wear.ui.login.LoginScreen
import com.woocommerce.wear.ui.mystore.MyStoreScreen

@Composable
fun WooWearNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = "login"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen()
        }
        composable("home") {
            MyStoreScreen()
        }
    }
}
