package com.woocommerce.android.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.woocommerce.android.ui.NavRoutes.LOGIN
import com.woocommerce.android.ui.NavRoutes.MY_STORE
import com.woocommerce.android.ui.login.LoginScreen
import com.woocommerce.android.ui.login.LoginViewModel
import com.woocommerce.android.ui.mystore.StoreStatsScreen
import com.woocommerce.android.ui.mystore.StoreStatsViewModel

@Composable
fun WooWearNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = LOGIN.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(LOGIN.route) {
            val viewModel: LoginViewModel = hiltViewModel<LoginViewModel, LoginViewModel.Factory> {
                it.create(navController)
            }
            LoginScreen(viewModel)
        }
        composable(MY_STORE.route) {
            val viewModel: StoreStatsViewModel = hiltViewModel<StoreStatsViewModel, StoreStatsViewModel.Factory> {
                it.create(navController)
            }
            StoreStatsScreen(viewModel)
        }
    }
}

enum class NavRoutes(val route: String) {
    LOGIN("login"),
    MY_STORE("myStore")
}
