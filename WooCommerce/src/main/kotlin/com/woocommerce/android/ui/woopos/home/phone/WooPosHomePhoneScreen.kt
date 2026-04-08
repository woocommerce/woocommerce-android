package com.woocommerce.android.ui.woopos.home.phone

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.woocommerce.android.ui.woopos.cardreader.connection.WooPosCardReaderConnectionDialog
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosExitConfirmationDialog
import com.woocommerce.android.ui.woopos.common.composeui.modifier.listenForBarcodes
import com.woocommerce.android.ui.woopos.home.WooPosHomeState
import com.woocommerce.android.ui.woopos.home.WooPosHomeUIEvent
import com.woocommerce.android.ui.woopos.home.WooPosHomeViewModel
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartViewModel
import com.woocommerce.android.ui.woopos.scanningsetup.WooPosScanningSetupDialog
import org.wordpress.android.util.ToastUtils

private const val PHONE_PRODUCTS_ROUTE = "phone_products"
private const val PHONE_CART_ROUTE = "phone_cart"
private const val PHONE_TOTALS_ROUTE = "phone_totals"

@Composable
fun WooPosHomePhoneScreen(
    isPaymentCompletedViaCash: Boolean,
    viewModel: WooPosHomeViewModel,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (isPaymentCompletedViaCash) {
            viewModel.onUIEvent(WooPosHomeUIEvent.OnPaymentCompletedViaCash)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            ToastUtils.showToast(context, message, ToastUtils.Duration.LONG)
        }
    }

    WooPosHomePhoneContent(
        state = state,
        onHomeUIEvent = { viewModel.onUIEvent(it) },
    )
}

@Composable
private fun WooPosHomePhoneContent(
    state: WooPosHomeState,
    onHomeUIEvent: (WooPosHomeUIEvent) -> Unit,
) {
    val navController = rememberNavController()

    // Capture the parent ViewModelStoreOwner (HOME_ROUTE NavBackStackEntry).
    // This ensures child screens in the inner NavHost share the same ViewModel instances
    // as they would on tablet where all screens are composed in the same scope.
    val parentViewModelStoreOwner = LocalViewModelStoreOwner.current!!

    // Eagerly create all child ViewModels so they start collecting SharedFlow events
    // immediately. On tablet, all screens are composed simultaneously. On phone, only one
    // screen is composed at a time, but the ViewModels must exist to receive events.
    val cartViewModel: WooPosCartViewModel = hiltViewModel()
    hiltViewModel<com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewModel>()
    hiltViewModel<com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel>()
    hiltViewModel<com.woocommerce.android.ui.woopos.home.toolbar.WooPosHomeFloatingToolbarViewModel>()

    val cartState = cartViewModel.state.observeAsState()
    val cartItemCount = cartState.value?.body?.amountOfItems ?: 0

    var previousState by remember {
        mutableStateOf<WooPosHomeState.ScreenPositionState?>(null)
    }

    LaunchedEffect(state.screenPositionState) {
        val currentRoute = navController.currentDestination?.route
        when (state.screenPositionState) {
            is WooPosHomeState.ScreenPositionState.Cart -> {
                when {
                    // Back from checkout: go to cart (pop one step)
                    currentRoute == PHONE_TOTALS_ROUTE &&
                        previousState is WooPosHomeState.ScreenPositionState.Checkout.CartWithTotals -> {
                        navController.popBackStack()
                    }
                    // Payment complete or other: go to products
                    currentRoute == PHONE_TOTALS_ROUTE || currentRoute == PHONE_CART_ROUTE -> {
                        navController.popBackStack(PHONE_PRODUCTS_ROUTE, inclusive = false)
                    }
                }
            }
            is WooPosHomeState.ScreenPositionState.Checkout -> {
                if (currentRoute != PHONE_TOTALS_ROUTE) {
                    navController.navigate(PHONE_TOTALS_ROUTE) {
                        launchSingleTop = true
                    }
                }
            }
        }
        previousState = state.screenPositionState
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .listenForBarcodes(
                onBarcodeEvent = { result ->
                    onHomeUIEvent(WooPosHomeUIEvent.OnBarcodeEvent(result))
                },
                enabled = (
                    state.screenPositionState is WooPosHomeState.ScreenPositionState.Cart ||
                        state.screenPositionState is WooPosHomeState.ScreenPositionState.Checkout.FullScreenTotals
                    ) && state.dialogState !is WooPosHomeState.DialogState.ScanningSetupDialog
            )
    ) {
        NavHost(
            navController = navController,
            startDestination = PHONE_PRODUCTS_ROUTE,
        ) {
            composable(PHONE_PRODUCTS_ROUTE) {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides parentViewModelStoreOwner
                ) {
                    BackHandler {
                        onHomeUIEvent(WooPosHomeUIEvent.SystemBackClicked)
                    }
                    WooPosPhoneProductsScreen(
                        cartItemCount = cartItemCount,
                        onCartClicked = {
                            navController.navigate(PHONE_CART_ROUTE) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }

            composable(PHONE_CART_ROUTE) {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides parentViewModelStoreOwner
                ) {
                    BackHandler {
                        navController.popBackStack()
                    }
                    WooPosPhoneCartScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            composable(PHONE_TOTALS_ROUTE) {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides parentViewModelStoreOwner
                ) {
                    BackHandler {
                        onHomeUIEvent(WooPosHomeUIEvent.SystemBackClicked)
                    }
                    WooPosPhoneTotalsScreen()
                }
            }
        }

        PhoneDialogs(state.dialogState, onHomeUIEvent)
    }
}

@Composable
private fun PhoneDialogs(
    dialogState: WooPosHomeState.DialogState,
    onHomeUIEvent: (WooPosHomeUIEvent) -> Unit
) {
    WooPosScanningSetupDialog(
        isVisible = dialogState is WooPosHomeState.DialogState.ScanningSetupDialog,
        onDismissRequest = {
            onHomeUIEvent(WooPosHomeUIEvent.DismissScanningSetupDialog)
        }
    )

    WooPosExitConfirmationDialog(
        isVisible = dialogState is WooPosHomeState.DialogState.ExitConfirmationDialog,
        title = stringResource(id = WooPosHomeState.DialogState.ExitConfirmationDialog.title),
        message = stringResource(id = WooPosHomeState.DialogState.ExitConfirmationDialog.message),
        dismissButtonText = stringResource(
            id = WooPosHomeState.DialogState.ExitConfirmationDialog.confirmButton
        ),
        onDismissRequest = { onHomeUIEvent(WooPosHomeUIEvent.ExitConfirmationDialogDismissed) },
        onExit = { onHomeUIEvent(WooPosHomeUIEvent.ExitPosClicked) }
    )

    if (dialogState is WooPosHomeState.DialogState.CardReaderConnectionDialog) {
        WooPosCardReaderConnectionDialog(
            onDismiss = { onHomeUIEvent(WooPosHomeUIEvent.DismissCardReaderConnectionDialog) },
            onConnectionSuccess = { onHomeUIEvent(WooPosHomeUIEvent.DismissCardReaderConnectionDialog) }
        )
    }
}
