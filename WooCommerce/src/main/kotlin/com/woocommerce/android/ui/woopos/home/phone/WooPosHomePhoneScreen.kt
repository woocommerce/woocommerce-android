package com.woocommerce.android.ui.woopos.home.phone

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.cardreader.connection.WooPosCardReaderConnectionDialog
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosExitConfirmationDialog
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.modifier.listenForBarcodes
import com.woocommerce.android.ui.woopos.home.WooPosHomeState
import com.woocommerce.android.ui.woopos.home.WooPosHomeUIEvent
import com.woocommerce.android.ui.woopos.home.WooPosHomeViewModel
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartState
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartUIEvent
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
    val totalsViewModel: com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewModel = hiltViewModel()
    hiltViewModel<com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel>()
    hiltViewModel<com.woocommerce.android.ui.woopos.home.toolbar.WooPosHomeFloatingToolbarViewModel>()

    val cartState = cartViewModel.state.observeAsState()
    val cartItemCount = cartState.value?.body?.amountOfItems ?: 0
    val cartItemsLabel = cartState.value?.toolbar?.itemsCount
    val cartFormattedSubtotal = cartState.value?.toolbar?.formattedSubtotal

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

    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    val isCheckoutEnabled =
        cartState.value?.checkoutButtonState == WooPosCartState.CheckoutButtonState.Enabled

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
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
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
                            WooPosPhoneProductsScreen()
                        }
                    }

                    composable(
                        PHONE_CART_ROUTE,
                        enterTransition = {
                            slideInVertically(animationSpec = tween(300), initialOffsetY = { it })
                        },
                        exitTransition = {
                            slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { -it })
                        },
                        popEnterTransition = {
                            slideInHorizontally(animationSpec = tween(300), initialOffsetX = { -it }) +
                                fadeIn(animationSpec = tween(150))
                        },
                        popExitTransition = {
                            slideOutVertically(animationSpec = tween(300), targetOffsetY = { it })
                        },
                    ) {
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

                    composable(
                        PHONE_TOTALS_ROUTE,
                        enterTransition = {
                            slideInHorizontally(animationSpec = tween(300), initialOffsetX = { it })
                        },
                        exitTransition = {
                            slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { -it })
                        },
                        popEnterTransition = {
                            slideInHorizontally(animationSpec = tween(300), initialOffsetX = { -it })
                        },
                        popExitTransition = {
                            slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { it })
                        },
                    ) {
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
            }

            PhonePersistentBottomButton(
                currentRoute = currentRoute,
                cartItemCount = cartItemCount,
                cartItemsLabel = cartItemsLabel,
                cartFormattedSubtotal = cartFormattedSubtotal,
                isCheckoutEnabled = isCheckoutEnabled,
                screenPositionState = state.screenPositionState,
                onCartClicked = {
                    navController.navigate(PHONE_CART_ROUTE) {
                        launchSingleTop = true
                    }
                },
                onCheckoutClicked = {
                    cartViewModel.onUIEvent(WooPosCartUIEvent.CheckoutClicked)
                },
                onCashPaymentClicked = {
                    totalsViewModel.onUIEvent(
                        com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsUIEvent.OnCashPaymentClicked
                    )
                },
            )
        }

        PhoneDialogs(state.dialogState, onHomeUIEvent)
    }
}

@Composable
private fun PhonePersistentBottomButton(
    currentRoute: String?,
    cartItemCount: Int,
    cartItemsLabel: String?,
    cartFormattedSubtotal: String?,
    isCheckoutEnabled: Boolean,
    screenPositionState: WooPosHomeState.ScreenPositionState,
    onCartClicked: () -> Unit,
    onCheckoutClicked: () -> Unit,
    onCashPaymentClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isVisible = when (currentRoute) {
        PHONE_PRODUCTS_ROUTE -> cartItemCount > 0
        PHONE_CART_ROUTE -> true
        PHONE_TOTALS_ROUTE ->
            screenPositionState !is WooPosHomeState.ScreenPositionState.Checkout.FullScreenTotals
        else -> false
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(animationSpec = tween(100)),
        modifier = modifier,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceBright,
            modifier = Modifier.fillMaxWidth()
        ) {
            val buttonText = when (currentRoute) {
                PHONE_PRODUCTS_ROUTE -> {
                    val label = cartFormattedSubtotal ?: cartItemsLabel ?: "$cartItemCount"
                    stringResource(R.string.woopos_cart_title) + " - $label"
                }
                PHONE_CART_ROUTE ->
                    stringResource(R.string.woopos_checkout_button)
                PHONE_TOTALS_ROUTE ->
                    stringResource(R.string.woopos_payment_take_cash_payment_label)
                else -> ""
            }
            val onClick = when (currentRoute) {
                PHONE_PRODUCTS_ROUTE -> onCartClicked
                PHONE_CART_ROUTE -> onCheckoutClicked
                PHONE_TOTALS_ROUTE -> onCashPaymentClicked
                else -> ({})
            }
            val buttonState = if (currentRoute == PHONE_CART_ROUTE && !isCheckoutEnabled) {
                WooPosButtonState.DISABLED
            } else {
                WooPosButtonState.ENABLED
            }

            if (currentRoute == PHONE_TOTALS_ROUTE) {
                WooPosOutlinedButton(
                    text = buttonText,
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WooPosSpacing.Medium.value)
                        .navigationBarsPadding()
                )
            } else {
                WooPosButton(
                    text = buttonText,
                    onClick = onClick,
                    state = buttonState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WooPosSpacing.Medium.value)
                        .navigationBarsPadding()
                )
            }
        }
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
