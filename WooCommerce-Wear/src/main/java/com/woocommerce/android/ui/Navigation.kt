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
import com.woocommerce.android.ui.mystore.MyStoreScreen
import com.woocommerce.android.ui.orders.OrdersListViewModel
import com.woocommerce.android.ui.stats.StoreStatsViewModel

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
            val storeStatsViewModel = hiltViewModel<StoreStatsViewModel, StoreStatsViewModel.Factory> {
                it.create(navController)
            }
            val ordersListViewModel = hiltViewModel<OrdersListViewModel, OrdersListViewModel.Factory> {
                it.create(navController)
            }
            MyStoreScreen(
                storeStatsViewModel = storeStatsViewModel,
                ordersListViewModel = ordersListViewModel
            )
        }
    }
}

enum class NavRoutes(val route: String) {
    LOGIN("login"),
    MY_STORE("myStore")
}
