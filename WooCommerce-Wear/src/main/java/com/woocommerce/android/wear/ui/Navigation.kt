package com.woocommerce.android.wear.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.woocommerce.android.wear.ui.NavArgs.ORDER_ID
import com.woocommerce.android.wear.ui.NavRoutes.LOGIN
import com.woocommerce.android.wear.ui.NavRoutes.MY_STORE
import com.woocommerce.android.wear.ui.NavRoutes.ORDER_DETAILS
import com.woocommerce.android.wear.ui.login.LoginScreen
import com.woocommerce.android.wear.ui.login.LoginViewModel
import com.woocommerce.android.wear.ui.orders.details.OrderDetailsScreen
import com.woocommerce.android.wear.ui.orders.details.OrderDetailsViewModel
import com.woocommerce.android.wear.ui.orders.list.OrdersListViewModel
import com.woocommerce.android.wear.ui.stats.StoreStatsViewModel

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
            val storeStatsViewModel = hiltViewModel<StoreStatsViewModel>()
            val ordersListViewModel = hiltViewModel<OrdersListViewModel, OrdersListViewModel.Factory> {
                it.create(navController)
            }
            MyStoreScreen(
                storeStatsViewModel = storeStatsViewModel,
                ordersListViewModel = ordersListViewModel
            )
        }
        composable(
            route = ORDER_DETAILS.withArgs(ORDER_ID),
            arguments = listOf(navArgument(ORDER_ID.key) { type = NavType.LongType })
        ) {
            val viewModel: OrderDetailsViewModel = hiltViewModel()
            OrderDetailsScreen(viewModel)
        }
    }
}

enum class NavRoutes(val route: String) {
    LOGIN("login"),
    MY_STORE("myStore"),
    ORDER_DETAILS("orderDetails");

    fun withArgs(args: Any): String {
        return "$route/$args"
    }

    fun withArgs(navArgs: NavArgs): String {
        return "$route/{${navArgs.key}}"
    }
}

enum class NavArgs(val key: String) {
    ORDER_ID("orderId")
}
