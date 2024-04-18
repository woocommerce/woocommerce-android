package com.woocommerce.wear.ui

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.woocommerce.wear.ui.login.LoginScreen

val nav = NavHost(
    navController = navController,
    startDestination = "login"
) {
    composable("login") {
        LoginScreen()
    }
    composable("home") {
        LandingScreen()
    }
}
