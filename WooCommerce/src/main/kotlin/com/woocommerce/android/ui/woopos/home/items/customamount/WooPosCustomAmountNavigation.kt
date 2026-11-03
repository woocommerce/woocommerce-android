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
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState
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

fun NavController.navigateToCustomAmountScreen(editing: WooPosCartItemViewState.CustomAmount?) {
    if (currentDestination?.route == CUSTOM_AMOUNT_ROUTE) return

    val route = if (editing == null) {
        CUSTOM_AMOUNT_ROUTE_BASE
    } else {
        "$CUSTOM_AMOUNT_ROUTE_BASE?" +
            "$ITEM_NUMBER_KEY=${editing.itemNumber}&" +
            "$AMOUNT_KEY=${Uri.encode(editing.amount.toPlainString())}&" +
            "$NAME_KEY=${Uri.encode(editing.name)}&" +
            "$IS_TAXABLE_KEY=${editing.isTaxable}"
    }
    navigate(route) { launchSingleTop = true }
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
            editing = backStackEntry.toEditingItem(),
            onNavigationEvent = onNavigationEvent,
        )
    }
}

private fun NavBackStackEntry.toEditingItem(): WooPosCartItemViewState.CustomAmount? {
    val args = arguments ?: return null
    val itemNumber = args.getInt(ITEM_NUMBER_KEY, NO_ITEM_NUMBER)
    val amount = args.getString(AMOUNT_KEY)?.toBigDecimalOrNull()
    if (itemNumber == NO_ITEM_NUMBER || amount == null) return null
    return WooPosCartItemViewState.CustomAmount(
        itemNumber = itemNumber,
        name = args.getString(NAME_KEY).orEmpty(),
        amount = amount,
        formattedAmount = "",
        isTaxable = args.getBoolean(IS_TAXABLE_KEY),
    )
}
