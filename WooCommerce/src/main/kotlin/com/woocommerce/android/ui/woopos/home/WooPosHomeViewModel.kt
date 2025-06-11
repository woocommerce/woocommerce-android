package com.woocommerce.android.ui.woopos.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.common.util.WooPosSoundHelper
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.NavigationEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent.CheckoutClicked
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent.CouponsRemoved
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent.ItemClickedInItemsList
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent.OrderCreated
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent.OrderSuccessfullyPaid.PaymentMethod
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent.SearchEvent.ChangedQuery
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent.SearchEvent.RecentSearchSelected
import com.woocommerce.android.ui.woopos.home.WooPosHomeState.BarcodeInfoDialog
import com.woocommerce.android.ui.woopos.home.WooPosHomeState.ExitConfirmationDialog
import com.woocommerce.android.ui.woopos.home.WooPosHomeState.ProductsInfoDialog
import com.woocommerce.android.ui.woopos.home.WooPosHomeState.ScreenPositionState
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
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
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val soundHelper: WooPosSoundHelper,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        key = "home_state",
        initialValue = WooPosHomeState(
            screenPositionState = ScreenPositionState.Cart,
            productsInfoDialog = ProductsInfoDialog(isVisible = false),
            barcodeInfoDialog = BarcodeInfoDialog(isVisible = false),
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
                            screenPositionState = ScreenPositionState.Cart
                        )
                        sendEventToChildren(ParentToChildrenEvent.BackFromCheckoutToCartClicked)
                        viewModelScope.launch {
                            analyticsTracker.track(BackToCartTapped)
                        }
                    }

                    ScreenPositionState.Checkout.FullScreenTotals -> {
                        _state.value = _state.value.copy(
                            screenPositionState = ScreenPositionState.Cart
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

            WooPosHomeUIEvent.DismissBarcodeInfoDialog -> {
                _state.value = _state.value.copy(
                    barcodeInfoDialog = BarcodeInfoDialog(isVisible = false)
                )
            }

            WooPosHomeUIEvent.OnPaymentCompletedViaCash -> onOrderSuccessfullyPaid(
                PaymentMethod.CASH
            )
            WooPosHomeUIEvent.ExitPosClicked -> {
                viewModelScope.launch {
                    _navigationEvent.emit(NavigationEvent.ExitPos)
                    analyticsTracker.track(WooPosAnalyticsEvent.Event.ExitConfirmed)
                }
            }
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun listenBottomEvents() {
        viewModelScope.launch {
            childrenToParentEventReceiver.events.collect { event ->
                when (event) {
                    is ChildToParentEvent.CheckoutClicked -> {
                        _state.value = _state.value.copy(
                            screenPositionState = ScreenPositionState.Checkout.CartWithTotals
                        )
                        sendEventToChildren(CheckoutClicked(event.itemClickedDataList))
                    }

                    is ChildToParentEvent.BackFromCheckoutToCartClicked -> {
                        _state.value = _state.value.copy(
                            screenPositionState = ScreenPositionState.Cart
                        )
                        sendEventToChildren(ParentToChildrenEvent.BackFromCheckoutToCartClicked)
                    }

                    is ChildToParentEvent.ItemClickedInItemsList -> {
                        sendEventToChildren(
                            ItemClickedInItemsList(
                                itemData = event.itemData,
                                eventForTracking = event.eventForTracking
                            )
                        )
                    }

                    is ChildToParentEvent.NewTransactionClicked -> {
                        _state.value = _state.value.copy(
                            screenPositionState = ScreenPositionState.Cart
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

                    ChildToParentEvent.SimpleProductExplanationMenuItemClicked -> {
                        _state.value = _state.value.copy(
                            productsInfoDialog = ProductsInfoDialog(isVisible = true)
                        )
                    }

                    ChildToParentEvent.BarcodeInfoMenuItemClicked -> {
                        _state.value = _state.value.copy(
                            barcodeInfoDialog = BarcodeInfoDialog(isVisible = true)
                        )
                    }

                    is ChildToParentEvent.ToastMessageDisplayed -> {
                        viewModelScope.launch {
                            _toastEvent.emit(event.message)
                        }
                    }

                    is NavigationEvent -> viewModelScope.launch { _navigationEvent.emit(event) }
                    is ChildToParentEvent.SearchEvent.QueryChanged -> {
                        sendEventToChildren(ChangedQuery(event.query))
                    }
                    ChildToParentEvent.SearchEvent.Finished -> {
                        sendEventToChildren(ParentToChildrenEvent.SearchEvent.Finished)
                    }
                    ChildToParentEvent.SearchEvent.Started -> {
                        sendEventToChildren(ParentToChildrenEvent.SearchEvent.Started)
                    }
                    is ChildToParentEvent.SearchEvent.RecentSearchSelected -> {
                        sendEventToChildren(RecentSearchSelected(event.query))
                    }

                    is ChildToParentEvent.OrderCreated -> handleOrderCreated(event)
                    is ChildToParentEvent.CouponsValidationFailed -> {
                        sendEventToChildren(ParentToChildrenEvent.CouponsValidationFailed)
                    }
                    is ChildToParentEvent.RemoveCouponsClicked -> {
                        sendEventToChildren(ParentToChildrenEvent.RemoveCouponsClicked)
                    }
                    is ChildToParentEvent.CouponsRemoved -> {
                        sendEventToChildren(CouponsRemoved(event.cartDataList))
                    }

                    ChildToParentEvent.RefreshProductList -> {
                        sendEventToChildren(ParentToChildrenEvent.RefreshProductList)
                    }

                    is ChildToParentEvent.BarcodeScanned -> sendEventToChildren(
                        ParentToChildrenEvent.BarcodeScanned(event.barcode)
                    )
                }
            }
        }
    }

    private fun handleOrderCreated(event: ChildToParentEvent.OrderCreated) {
        sendEventToChildren(
            OrderCreated(
                updatedProducts = event.updatedProducts.map {
                    when (it) {
                        is ChildToParentEvent.OrderCreated.ProductInfo.Simple -> {
                            OrderCreated.ProductInfo.Simple(
                                id = it.id,
                                name = it.name,
                                finalPrice = it.finalPrice,
                                basePrice = it.basePrice,
                                quantity = it.quantity
                            )
                        }

                        is ChildToParentEvent.OrderCreated.ProductInfo.Variation -> {
                            OrderCreated.ProductInfo.Variation(
                                id = it.id,
                                name = it.name,
                                finalPrice = it.finalPrice,
                                basePrice = it.basePrice,
                                quantity = it.quantity,
                                variationId = it.variationId
                            )
                        }
                    }
                },
                updatedCoupons = event.updatedCoupons.map {
                    OrderCreated.CouponInfo(
                        id = it.id,
                        code = it.code,
                        discountAmount = it.discountAmount
                    )
                }
            )
        )
    }

    private fun sendEventToChildren(event: ParentToChildrenEvent) {
        viewModelScope.launch {
            parentToChildrenEventSender.sendToChildren(event)
        }
    }

    private fun onOrderSuccessfullyPaid(paymentMethod: PaymentMethod) {
        viewModelScope.launch {
            soundHelper.playChaChing()
        }
        _state.value = _state.value.copy(
            screenPositionState = ScreenPositionState.Checkout.FullScreenTotals
        )
        sendEventToChildren(ParentToChildrenEvent.OrderSuccessfullyPaid(paymentMethod))
    }
}
