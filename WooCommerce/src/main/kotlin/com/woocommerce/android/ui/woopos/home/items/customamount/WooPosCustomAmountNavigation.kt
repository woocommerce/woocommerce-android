package com.woocommerce.android.ui.woopos.home.items.customamount

import android.net.Uri
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.woocommerce.android.ui.woopos.home.HOME_ROUTE
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

private const val CUSTOM_AMOUNT_ROUTE_BASE = "$HOME_ROUTE/custom_amount"
private const val ITEM_NUMBER_KEY = "itemNumber"
private const val AMOUNT_KEY = "amount"
private const val NAME_KEY = "name"
private const val IS_TAXABLE_KEY = "isTaxable"
private const val NO_ITEM_NUMBER = -1

const val CUSTOM_AMOUNT_ROUTE = "$CUSTOM_AMOUNT_ROUTE_BASE?" +
    "$ITEM_NUMBER_KEY={$ITEM_NUMBER_KEY}&" +
    "$AMOUNT_KEY={$AMOUNT_KEY}&" +
    "$NAME_KEY={$NAME_KEY}&" +
    "$IS_TAXABLE_KEY={$IS_TAXABLE_KEY}"

fun NavController.navigateToCustomAmountScreen(mode: WooPosCustomAmountFormState.Mode) {
    if (currentDestination?.route == CUSTOM_AMOUNT_ROUTE) return

    val route = when (mode) {
        WooPosCustomAmountFormState.Mode.Add -> CUSTOM_AMOUNT_ROUTE_BASE
        is WooPosCustomAmountFormState.Mode.Edit ->
            "$CUSTOM_AMOUNT_ROUTE_BASE?" +
                "$ITEM_NUMBER_KEY=${mode.itemNumber}&" +
                "$AMOUNT_KEY=${Uri.encode(mode.amount.toPlainString())}&" +
                "$NAME_KEY=${Uri.encode(mode.name)}&" +
                "$IS_TAXABLE_KEY=${mode.isTaxable}"
    }
    navigate(route) { launchSingleTop = true }
}

fun NavController.closeCustomAmountScreen() {
    popBackStack(CUSTOM_AMOUNT_ROUTE, inclusive = true)
}

fun NavGraphBuilder.customAmountScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(
        route = CUSTOM_AMOUNT_ROUTE,
        arguments = listOf(
            navArgument(ITEM_NUMBER_KEY) {
                type = NavType.IntType
                defaultValue = NO_ITEM_NUMBER
            },
            navArgument(AMOUNT_KEY) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument(NAME_KEY) {
                type = NavType.StringType
                defaultValue = ""
            },
            navArgument(IS_TAXABLE_KEY) {
                type = NavType.BoolType
                defaultValue = false
            },
        ),
        enterTransition = {
            slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth })
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth })
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth })
        },
    ) { backStackEntry ->
        WooPosCustomAmountScreen(
            mode = backStackEntry.toMode(),
            onNavigationEvent = onNavigationEvent,
        )
    }
}

private fun NavBackStackEntry.toMode(): WooPosCustomAmountFormState.Mode {
    val args = arguments ?: return WooPosCustomAmountFormState.Mode.Add
    val itemNumber = args.getInt(ITEM_NUMBER_KEY, NO_ITEM_NUMBER)
    val amount = args.getString(AMOUNT_KEY)?.toBigDecimalOrNull()
    if (itemNumber == NO_ITEM_NUMBER || amount == null) return WooPosCustomAmountFormState.Mode.Add
    return WooPosCustomAmountFormState.Mode.Edit(
        itemNumber = itemNumber,
        name = args.getString(NAME_KEY).orEmpty(),
        amount = amount,
        isTaxable = args.getBoolean(IS_TAXABLE_KEY),
    )
}
