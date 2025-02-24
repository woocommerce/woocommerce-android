package com.woocommerce.android.ui.woopos.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.NavigationEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent.OrderSuccessfullyPaid.PaymentMethod
import com.woocommerce.android.ui.woopos.home.WooPosHomeState.ExitConfirmationDialog
import com.woocommerce.android.ui.woopos.home.WooPosHomeState.ProductsInfoDialog
import com.woocommerce.android.ui.woopos.home.WooPosHomeState.ScreenPositionState
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BackToCartTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosHomeViewModel @Inject constructor(
    private val childrenToParentEventReceiver: WooPosChildrenToParentEventReceiver,
    private val parentToChildrenEventSender: WooPosParentToChildrenEventSender,
    private val wooPosItemsNavigator: WooPosItemsNavigator,
    private val analyticsTracker: WooPosAnalyticsTracker,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        key = "home_state",
        initialValue = WooPosHomeState(
            screenPositionState = ScreenPositionState.Cart.Visible,
            productsInfoDialog = ProductsInfoDialog(isVisible = false),
            exitConfirmationDialog = ExitConfirmationDialog(isVisible = false),
        )
    )
    val state: StateFlow<WooPosHomeState> = _state

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent

    init {
        listenBottomEvents()
    }

    fun onUIEvent(event: WooPosHomeUIEvent) {
        when (event) {
            WooPosHomeUIEvent.SystemBackClicked -> {
                when (_state.value.screenPositionState) {
                    ScreenPositionState.Checkout.CartWithTotals -> {
                        _state.value = _state.value.copy(
                            screenPositionState = ScreenPositionState.Cart.Visible
                        )
                        sendEventToChildren(ParentToChildrenEvent.BackFromCheckoutToCartClicked)
                        viewModelScope.launch {
                            analyticsTracker.track(BackToCartTapped)
                        }
                    }

                    ScreenPositionState.Checkout.FullScreenTotals -> {
                        _state.value = _state.value.copy(
                            screenPositionState = ScreenPositionState.Cart.Visible
                        )
                    }

                    is ScreenPositionState.Cart -> {
                        _state.value = _state.value.copy(
                            exitConfirmationDialog = ExitConfirmationDialog(isVisible = true)
                        )
                    }
                }
            }

            WooPosHomeUIEvent.ExitConfirmationDialogDismissed -> {
                _state.value = _state.value.copy(
                    exitConfirmationDialog = ExitConfirmationDialog(isVisible = false)
                )
            }

            WooPosHomeUIEvent.DismissProductsInfoDialog -> {
                _state.value = _state.value.copy(
                    productsInfoDialog = ProductsInfoDialog(isVisible = false)
                )
            }

            WooPosHomeUIEvent.OnPaymentCompletedViaCash -> onOrderSuccessfullyPaid(
                PaymentMethod.CASH
            )
            WooPosHomeUIEvent.ExitPosClicked -> {
                viewModelScope.launch {
                    _navigationEvent.emit(NavigationEvent.ExitPos)
                }
            }
        }
    }

    @Suppress("LongMethod")
    private fun listenBottomEvents() {
        viewModelScope.launch {
            childrenToParentEventReceiver.events.collect { event ->
                when (event) {
                    is ChildToParentEvent.CheckoutClicked -> {
                        _state.value = _state.value.copy(
                            screenPositionState = ScreenPositionState.Checkout.CartWithTotals
                        )
                        sendEventToChildren(ParentToChildrenEvent.CheckoutClicked(event.itemClickedDataList))
                    }

                    is ChildToParentEvent.BackFromCheckoutToCartClicked -> {
                        _state.value = _state.value.copy(
                            screenPositionState = ScreenPositionState.Cart.Visible
                        )
                        sendEventToChildren(ParentToChildrenEvent.BackFromCheckoutToCartClicked)
                    }

                    is ChildToParentEvent.ItemClickedInProductSelector -> {
                        sendEventToChildren(
                            ParentToChildrenEvent.ItemClickedInProductSelector(event.itemData)
                        )
                    }

                    is ChildToParentEvent.NewTransactionClicked -> {
                        _state.value = _state.value.copy(
                            screenPositionState = ScreenPositionState.Cart.Visible
                        )
                    }

                    is ChildToParentEvent.OrderSuccessfullyPaidByCard -> onOrderSuccessfullyPaid(
                        PaymentMethod.CARD
                    )

                    is ChildToParentEvent.PaymentCollecting -> {
                        _state.value = _state.value.copy(
                            screenPositionState = ScreenPositionState.Checkout.CartWithTotals
                        )
                    }
                    is ChildToParentEvent.PaymentInProgress,
                    is ChildToParentEvent.PaymentFailed -> {
                        _state.value = _state.value.copy(
                            screenPositionState = ScreenPositionState.Checkout.FullScreenTotals
                        )
                    }

                    is ChildToParentEvent.GoBackToCheckoutAfterFailedPayment,
                    is ChildToParentEvent.ReturnedFromCardReaderPaymentToCheckout -> {
                        _state.value = _state.value.copy(
                            screenPositionState = ScreenPositionState.Checkout.CartWithTotals
                        )
                    }

                    ChildToParentEvent.ExitPosClicked -> {
                        _state.value = _state.value.copy(
                            exitConfirmationDialog = ExitConfirmationDialog(isVisible = true)
                        )
                    }

                    is ChildToParentEvent.ProductsStatusChanged -> handleProductsStatusChanged(event)

                    ChildToParentEvent.ProductsDialogInfoIconClicked -> {
                        _state.value = _state.value.copy(
                            productsInfoDialog = ProductsInfoDialog(isVisible = true)
                        )
                    }

                    is ChildToParentEvent.ToastMessageDisplayed -> {
                        viewModelScope.launch {
                            _toastEvent.emit(event.message)
                        }
                    }

                    is NavigationEvent -> viewModelScope.launch { _navigationEvent.emit(event) }
                }
            }
        }
    }

    private fun handleProductsStatusChanged(event: ChildToParentEvent.ProductsStatusChanged) {
        val screenPosition = _state.value.screenPositionState
        val newScreenPositionState = when (event) {
            ChildToParentEvent.ProductsStatusChanged.FullScreen -> {
                when (screenPosition) {
                    is ScreenPositionState.Cart -> ScreenPositionState.Cart.Hidden
                    is ScreenPositionState.Checkout -> screenPosition
                }
            }
            ChildToParentEvent.ProductsStatusChanged.WithCart -> {
                when (screenPosition) {
                    ScreenPositionState.Cart.Hidden -> ScreenPositionState.Cart.Visible

                    ScreenPositionState.Cart.Visible,
                    ScreenPositionState.Checkout.CartWithTotals,
                    ScreenPositionState.Checkout.FullScreenTotals -> screenPosition
                }
            }
        }
        _state.value = _state.value.copy(screenPositionState = newScreenPositionState)
    }

    private fun sendEventToChildren(event: ParentToChildrenEvent) {
        viewModelScope.launch {
            parentToChildrenEventSender.sendToChildren(event)
        }
    }

    private fun onOrderSuccessfullyPaid(paymentMethod: PaymentMethod) {
        viewModelScope.launch {
            wooPosItemsNavigator.sendNavigationEvent(
                WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateBackToItemListScreen
            )
        }
        _state.value = _state.value.copy(
            screenPositionState = ScreenPositionState.Checkout.FullScreenTotals
        )
        sendEventToChildren(ParentToChildrenEvent.OrderSuccessfullyPaid(paymentMethod))
    }
}
