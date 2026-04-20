package com.woocommerce.android.ui.woopos.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.woocommerce.android.ui.woopos.eligibility.WooPosEligibilityScreen
import com.woocommerce.android.ui.woopos.home.phone.WooPosHomePhoneScreen
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.root.navigation.navigateOnce
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.ui.woopos.util.ext.isWooPosPhoneLayout

const val HOME_ROUTE = "home"
const val HOME_PAYMENT_COMPLETED_VIA_CASH_KEY = "home_payment_completed_via_cash_key"
const val ELIGIBILITY_ROUTE = "eligibility"
const val ELIGIBILITY_REASON_KEY = "eligibility_reason"

fun NavController.navigateToHomeScreen() {
    navigateOnce(HOME_ROUTE)
}

fun NavController.navigateToHomeScreenAfterSuccessfulCashPayment() {
    previousBackStackEntry
        ?.savedStateHandle
        ?.set(HOME_PAYMENT_COMPLETED_VIA_CASH_KEY, true)

    navigate(HOME_ROUTE) {
        popUpTo(HOME_ROUTE) { inclusive = false }
        launchSingleTop = true
    }
}

fun NavController.navigateToHomeScreenIfHomeScreenNotOpen() {
    if (currentDestination?.route != HOME_ROUTE) {
        popBackStack(
            HOME_ROUTE,
            false
        )
    }
}

fun NavGraphBuilder.homeScreen(
    homeViewModel: WooPosHomeViewModel,
) {
    composable(
        route = HOME_ROUTE,
        enterTransition = {
            fadeIn(
                animationSpec = tween(
                    durationMillis = 300,
                    delayMillis = 200,
                    easing = FastOutSlowInEasing
                )
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth },
            )
        },
        popEnterTransition = {
            if (targetState.IsHomePaymentCompletedViaCash) {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                )
            } else {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                )
            }
        },
    ) { entry ->
        val savedStateHandle = entry.savedStateHandle

        val isPaymentCompletedViaCash = savedStateHandle.get<Boolean>(HOME_PAYMENT_COMPLETED_VIA_CASH_KEY) == true
        if (isPaymentCompletedViaCash) {
            savedStateHandle[HOME_PAYMENT_COMPLETED_VIA_CASH_KEY] = false
        }

        if (LocalContext.current.isWooPosPhoneLayout()) {
            WooPosHomePhoneScreen(
                isPaymentCompletedViaCash = isPaymentCompletedViaCash,
                viewModel = homeViewModel,
            )
        } else {
            WooPosHomeScreen(
                isPaymentCompletedViaCash = isPaymentCompletedViaCash,
                viewModel = homeViewModel,
            )
        }
    }
}

fun NavController.navigateToEligibilityScreen(reason: WooPosLaunchability.NonLaunchabilityReason) {
    navigate("$ELIGIBILITY_ROUTE/${reason.name}")
}

fun NavGraphBuilder.eligibilityScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
) {
    composable(
        route = "$ELIGIBILITY_ROUTE/{$ELIGIBILITY_REASON_KEY}",
        arguments = listOf(
            navArgument(ELIGIBILITY_REASON_KEY) { type = NavType.StringType }
        )
    ) { entry ->
        val reasonName = entry.arguments?.getString(ELIGIBILITY_REASON_KEY)
        val reason = reasonName?.let { WooPosLaunchability.NonLaunchabilityReason.valueOf(it) }
            ?: WooPosLaunchability.NonLaunchabilityReason.SiteSettingsUnavailable

        WooPosEligibilityScreen(
            initialReason = reason,
            onNavigationEvent = onNavigationEvent
        )
    }
}

val NavBackStackEntry.IsHomePaymentCompletedViaCash: Boolean
    get() = savedStateHandle.get<Boolean>(HOME_PAYMENT_COMPLETED_VIA_CASH_KEY) == true
