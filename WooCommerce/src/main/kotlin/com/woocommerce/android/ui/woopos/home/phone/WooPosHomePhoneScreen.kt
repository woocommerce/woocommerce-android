package com.woocommerce.android.ui.woopos.home.phone

import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.home.WooPosHomeDialogs
import com.woocommerce.android.ui.woopos.home.WooPosHomeState
import com.woocommerce.android.ui.woopos.home.WooPosHomeUIEvent
import com.woocommerce.android.ui.woopos.home.WooPosHomeViewModel
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartState
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartViewModel
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewModel
import com.woocommerce.android.ui.woopos.home.wooPosHomeRootContainer
import com.woocommerce.android.util.PackageUtils
import kotlinx.parcelize.Parcelize
import org.wordpress.android.util.ToastUtils

@Parcelize
private enum class PhoneScreen : Parcelable { Products, Cart, Totals }

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
    val itemsViewModel: WooPosItemsViewModel = hiltViewModel()
    val cartViewModel: WooPosCartViewModel = hiltViewModel()
    val totalsViewModel: WooPosTotalsViewModel = hiltViewModel()

    val cartState by cartViewModel.state.observeAsState()

    var screen by rememberSaveable { mutableStateOf(PhoneScreen.Products) }

    LaunchedEffect(state.screenPositionState) {
        when (state.screenPositionState) {
            is WooPosHomeState.ScreenPositionState.Checkout -> {
                screen = PhoneScreen.Totals
            }
            is WooPosHomeState.ScreenPositionState.Cart -> {
                if (screen == PhoneScreen.Totals) {
                    val cartHasItems = cartState?.body is WooPosCartState.Body.WithItems
                    screen = if (cartHasItems) PhoneScreen.Cart else PhoneScreen.Products
                }
            }
        }
    }

    BackHandler {
        when (screen) {
            PhoneScreen.Products -> onHomeUIEvent(WooPosHomeUIEvent.SystemBackClicked)
            PhoneScreen.Cart -> screen = PhoneScreen.Products
            PhoneScreen.Totals -> onHomeUIEvent(WooPosHomeUIEvent.SystemBackClicked)
        }
    }

    Box(
        modifier = Modifier.wooPosHomeRootContainer(state, onHomeUIEvent)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (screen) {
                PhoneScreen.Products -> {
                    WooPosPhoneProductsScreen(itemsViewModel = itemsViewModel)

                    if (PackageUtils.isDebugBuild()) {
                        // Temporary trigger so Cart is reachable until the persistent
                        // bottom button lands in WOOMOB-2657. Remove with that ticket.
                        ExtendedFloatingActionButton(
                            text = { WooPosText(text = "Cart (debug)", style = WooPosTypography.BodyLarge) },
                            icon = {},
                            onClick = { screen = PhoneScreen.Cart },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(WooPosSpacing.Medium.value)
                        )
                    }
                }
                PhoneScreen.Cart -> WooPosPhoneCartScreen(
                    onBack = { screen = PhoneScreen.Products },
                    viewModel = cartViewModel,
                )
                PhoneScreen.Totals -> WooPosPhoneTotalsScreen(viewModel = totalsViewModel)
            }
        }

        WooPosHomeDialogs(state.dialogState, onHomeUIEvent)
    }
}
