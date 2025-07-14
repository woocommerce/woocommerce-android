package com.woocommerce.android.apifaker.ui

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.woocommerce.android.apifaker.ui.details.EndpointDetailsScreen
import com.woocommerce.android.apifaker.ui.details.MISSING_ENDPOINT_ID
import com.woocommerce.android.apifaker.ui.home.HomeScreen
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@Composable
fun ApiFakerNavHost(
    onExit: () -> Unit
) {
    val navController = rememberNavController()
    val snackbarHostStateEntryPoint = SnackbarHostStateEntryPoint.create()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route()
    ) {
        composable(Screen.Home.route()) {
            HomeScreen(
                viewModel = hiltViewModel(),
                navController = navController,
                snackbarHostState = snackbarHostStateEntryPoint.snackbarHostState(),
                onExit = onExit
            )
        }
        composable(
            Screen.EndpointDetails.routeTemplate,
            arguments = listOf(
                navArgument(Screen.EndpointDetails.endpointIdArgumentName) {
                    type = NavType.LongType
                    defaultValue = MISSING_ENDPOINT_ID
                }
            )
        ) {
            EndpointDetailsScreen(
                viewModel = hiltViewModel(),
                navController = navController,
                snackbarHostState = snackbarHostStateEntryPoint.snackbarHostState()
            )
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SnackbarHostStateEntryPoint {
    fun snackbarHostState(): SnackbarHostState

    companion object {
        @Composable
        fun create(): SnackbarHostStateEntryPoint {
            val appContext = LocalContext.current.applicationContext ?: error("No context found")

            return EntryPointAccessors.fromApplication(
                appContext,
                SnackbarHostStateEntryPoint::class.java
            )
        }
    }
}
