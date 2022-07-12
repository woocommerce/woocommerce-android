package com.woocommerce.android.ui.orders.creation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R.string
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_CONTEXT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_DESC
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_TYPE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_CUSTOMER_DETAILS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_FEES
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_SHIPPING_METHOD
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PRODUCT_COUNT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_STATUS
import com.woocommerce.android.ui.orders.creation.CreateUpdateOrder.OrderUpdateStatus
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget.ShowCreatedOrder
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@ExperimentalCoroutinesApi
class OrderCreationViewModel @Inject constructor(
    savedState: SavedStateHandle,
    dispatchers: CoroutineDispatchers,
    orderDetailRepository: OrderDetailRepository,
    mapItemToProductUiModel: MapItemToProductUiModel,
    createOrderItem: CreateOrderItem,
    private val orderCreationRepository: OrderCreationRepository,
    private val determineMultipleLinesContext: DetermineMultipleLinesContext,
    autoSyncPriceModifier: AutoSyncPriceModifier,
    parameterRepository: ParameterRepository
) : OrderCreateEditViewModel(
    savedState,
    dispatchers,
    orderDetailRepository,
    mapItemToProductUiModel,
    createOrderItem,
) {
    override val syncStrategy: SyncStrategy = autoSyncPriceModifier
    init {
        _orderDraft.update {
            it.copy(currency = parameterRepository.getParameters(PARAMETERS_KEY, savedState).currencyCode.orEmpty())
        }
        monitorOrderChanges()
    }

    override fun onSaveOrderClicked() {
        val order = _orderDraft.value
        trackSaveOrderButtonClick()
        viewModelScope.launch {
            viewState = viewState.copy(isProgressDialogShown = true)
            orderCreationRepository.placeOrder(order).fold(
                onSuccess = {
                    AnalyticsTracker.track(AnalyticsEvent.ORDER_CREATION_SUCCESS)
                    triggerEvent(ShowSnackbar(string.order_creation_success_snackbar))
                    triggerEvent(ShowCreatedOrder(it.id))
                },
                onFailure = {
                    trackOrderSaveFailure(it)
                    viewState = viewState.copy(isProgressDialogShown = false)
                    triggerEvent(ShowSnackbar(string.order_creation_failure_snackbar))
                }
            )
        }

    }

    override fun onBackButtonClicked() {
        if (_orderDraft.value.isEmpty()) {
            triggerEvent(Exit)
        } else {
            triggerEvent(
                ShowDialog.buildDiscardDialogEvent(
                    positiveBtnAction = { _, _ ->
                        val draft = _orderDraft.value
                        if (draft.id != 0L) {
                            launch { orderCreationRepository.deleteDraftOrder(draft) }
                        }
                        triggerEvent(Exit)
                    }
                )
            )
        }
    }

    /**
     * Monitor order changes, and update the remote draft to update price totals
     */
    override fun monitorOrderChanges() {
        viewModelScope.launch {
            syncStrategy.syncOrderChanges(_orderDraft, retryOrderDraftUpdateTrigger)
                .collect { updateStatus ->
                    when (updateStatus) {
                        OrderUpdateStatus.PendingDebounce ->
                            viewState = viewState.copy(willUpdateOrderDraft = true, showOrderUpdateSnackbar = false)
                        OrderUpdateStatus.Ongoing ->
                            viewState = viewState.copy(willUpdateOrderDraft = false, isUpdatingOrderDraft = true)
                        OrderUpdateStatus.Failed ->
                            viewState = viewState.copy(isUpdatingOrderDraft = false, showOrderUpdateSnackbar = true)
                        is OrderUpdateStatus.Succeeded -> {
                            viewState = viewState.copy(
                                isUpdatingOrderDraft = false,
                                showOrderUpdateSnackbar = false,
                                isEditable = true,
                                multipleLinesContext = determineMultipleLinesContext(updateStatus.order)
                            )
                            _orderDraft.update { currentDraft ->
                                // Keep the user's selected status
                                updateStatus.order.copy(status = currentDraft.status)
                            }
                        }
                    }
                }
        }
    }

    override fun trackOrderSaveFailure(it: Throwable) {
        AnalyticsTracker.track(
            AnalyticsEvent.ORDER_CREATION_FAILED,
            mapOf(
                KEY_ERROR_CONTEXT to this::class.java.simpleName,
                KEY_ERROR_TYPE to (it as? WooException)?.error?.type?.name,
                KEY_ERROR_DESC to it.message
            )
        )
    }

    override fun trackSaveOrderButtonClick() {
        AnalyticsTracker.track(
            AnalyticsEvent.ORDER_CREATE_BUTTON_TAPPED,
            mapOf(
                KEY_STATUS to _orderDraft.value.status,
                KEY_PRODUCT_COUNT to products.value?.count(),
                KEY_HAS_CUSTOMER_DETAILS to _orderDraft.value.billingAddress.hasInfo(),
                KEY_HAS_FEES to _orderDraft.value.feesLines.isNotEmpty(),
                KEY_HAS_SHIPPING_METHOD to _orderDraft.value.shippingLines.isNotEmpty()
            )
        )
    }
}
