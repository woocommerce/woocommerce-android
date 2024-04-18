package com.woocommerce.wear.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.woocommerce.wear.ui.login.LoginScreen
import com.woocommerce.wear.ui.login.LoginViewModel
import com.woocommerce.wear.ui.mystore.MyStoreScreen

@Composable
fun WooWearNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = "login"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            val viewModel: LoginViewModel = hiltViewModel<LoginViewModel, LoginViewModel.Factory> {
                it.create(navController)
            }
            LoginScreen(viewModel)
        }
        composable("myStore") {
            MyStoreScreen()
        }
    }
}
