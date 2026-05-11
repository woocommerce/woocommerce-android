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
import com.woocommerce.android.ui.woopos.home.WooPosHomeState.DialogState
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
            screenPositionState = ScreenPositionState.Products,
            dialogState = DialogState.Hidden,
        )
    )
    val state: StateFlow<WooPosHomeState> = _state

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent

    init {
        listenBottomEvents()
        viewModelScope.launch {
            soundHelper.preloadChaChing()
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundHelper.onCleanup()
    }

    fun onUIEvent(event: WooPosHomeUIEvent) {
        when (event) {
            WooPosHomeUIEvent.SystemBackClicked -> handleSystemBackClicked()

            WooPosHomeUIEvent.PhoneOpenCartClicked -> {
                _state.value = _state.value.copy(
                    screenPositionState = ScreenPositionState.Cart
                )
            }

            WooPosHomeUIEvent.PhoneBackFromCartClicked -> {
                _state.value = _state.value.copy(
                    screenPositionState = ScreenPositionState.Products
                )
            }

            WooPosHomeUIEvent.PhoneBackFromCheckoutClicked -> handleSystemBackClicked()

            WooPosHomeUIEvent.ExitConfirmationDialogDismissed -> {
                _state.value = _state.value.copy(
                    dialogState = DialogState.Hidden
                )
            }

            WooPosHomeUIEvent.DismissScanningSetupDialog -> {
                _state.value = _state.value.copy(
                    dialogState = DialogState.Hidden
                )
            }

            WooPosHomeUIEvent.DismissCardReaderConnectionDialog -> {
                _state.value = _state.value.copy(
                    dialogState = DialogState.Hidden
                )
            }

            WooPosHomeUIEvent.DismissCustomAmountDialog -> {
                _state.value = _state.value.copy(
                    dialogState = DialogState.Hidden
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

            is WooPosHomeUIEvent.OnBarcodeEvent -> {
                sendEventToChildren(ParentToChildrenEvent.BarcodeEvent(event.result))
            }
        }
    }

    private fun handleSystemBackClicked() {
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
                sendEventToChildren(ParentToChildrenEvent.BackFromCheckoutToCartClicked)
            }

            ScreenPositionState.Products,
            is ScreenPositionState.Cart -> {
                when (_state.value.dialogState) {
                    DialogState.Hidden -> {
                        _state.value = _state.value.copy(
                            dialogState = DialogState.ExitConfirmationDialog
                        )
                    }
                    else -> {
                        _state.value = _state.value.copy(
                            dialogState = DialogState.Hidden
                        )
                    }
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

                    is ChildToParentEvent.OnNewTransactionStarted -> {
                        if (_state.value.screenPositionState !is ScreenPositionState.Products) {
                            _state.value = _state.value.copy(
                                screenPositionState = ScreenPositionState.Products
                            )
                        }
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
                            dialogState = DialogState.ExitConfirmationDialog
                        )
                    }

                    ChildToParentEvent.SetupBarcodeScannerClicked -> {
                        _state.value = _state.value.copy(
                            dialogState = DialogState.ScanningSetupDialog
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

                    is ChildToParentEvent.OrderCreated -> {
                        sendEventToChildren(OrderCreated(event.data))
                    }
                    is ChildToParentEvent.CouponsValidationFailed -> {
                        sendEventToChildren(ParentToChildrenEvent.CouponsValidationFailed)
                    }

                    is ChildToParentEvent.MissingVariationEvent -> {
                        sendEventToChildren(ParentToChildrenEvent.MissingVariationEvent(event.variationId))
                    }

                    is ChildToParentEvent.RemoveCouponsClicked -> {
                        sendEventToChildren(ParentToChildrenEvent.RemoveCouponsClicked)
                    }

                    is ChildToParentEvent.CouponsRemoved -> {
                        sendEventToChildren(CouponsRemoved(event.cartDataList))
                    }

                    is ChildToParentEvent.RemoveProductsClicked -> {
                        sendEventToChildren(ParentToChildrenEvent.RemoveProductsClicked(event.removedProductsIds))
                    }

                    is ChildToParentEvent.ProductsRemoved -> {
                        sendEventToChildren(ParentToChildrenEvent.ProductsRemoved(event.cartDataList))
                    }

                    ChildToParentEvent.RefreshProductList -> {
                        sendEventToChildren(ParentToChildrenEvent.RefreshProductList)
                    }

                    ChildToParentEvent.ShowCardReaderConnectionDialog -> {
                        _state.value = _state.value.copy(
                            dialogState = DialogState.CardReaderConnectionDialog
                        )
                    }

                    is ChildToParentEvent.CustomAmountDialogRequested -> {
                        _state.value = _state.value.copy(
                            dialogState = DialogState.CustomAmountDialog(editing = event.editing)
                        )
                    }

                    is ChildToParentEvent.CustomAmountSubmitted -> {
                        _state.value = _state.value.copy(dialogState = DialogState.Hidden)
                        sendEventToChildren(
                            ParentToChildrenEvent.CustomAmountSubmitted(
                                name = event.name,
                                amount = event.amount,
                                isTaxable = event.isTaxable,
                                editingItemNumber = event.editingItemNumber,
                            )
                        )
                    }

                    is ChildToParentEvent.SettingsEvent -> Unit
                }
            }
        }
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
