package com.woocommerce.android.ui.orders.creation

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@ExperimentalCoroutinesApi
class OrderEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    dispatchers: CoroutineDispatchers,
    orderDetailRepository: OrderDetailRepository,
    mapItemToProductUiModel: MapItemToProductUiModel,
    createOrderItem: CreateOrderItem,
    private val determineMultipleLinesContext: DetermineMultipleLinesContext,
    parameterRepository: ParameterRepository,
    autoSyncOrder: AutoSyncOrder,
) : OrderCreateEditViewModel(
    savedState,
    dispatchers,
    orderDetailRepository,
    mapItemToProductUiModel,
    createOrderItem,
) {
    private val args: OrderCreationFormFragmentArgs by savedState.navArgs()
    override val syncStrategy: SyncStrategy = autoSyncOrder

    init {
        viewModelScope.launch {
            val currency = parameterRepository.getParameters(PARAMETERS_KEY, savedState).currencyCode.orEmpty()
            val orderId = (args.mode as Mode.Edit).orderId
            orderDetailRepository.getOrderById(orderId)?.let {
                _orderDraft.value = it.copy(currency = currency)
                monitorOrderChanges()
            }
        }
    }

    override fun onSaveOrderClicked() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    override fun onBackButtonClicked() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    override fun monitorOrderChanges() {
        viewModelScope.launch {
            syncStrategy.syncOrderChanges(_orderDraft, retryOrderDraftUpdateTrigger)
                .collect { updateStatus ->
                    when (updateStatus) {
                        CreateUpdateOrder.OrderUpdateStatus.PendingDebounce ->
                            viewState = viewState.copy(willUpdateOrderDraft = true, showOrderUpdateSnackbar = false)
                        CreateUpdateOrder.OrderUpdateStatus.Ongoing ->
                            viewState = viewState.copy(willUpdateOrderDraft = false, isUpdatingOrderDraft = true)
                        CreateUpdateOrder.OrderUpdateStatus.Failed ->
                            viewState = viewState.copy(isUpdatingOrderDraft = false, showOrderUpdateSnackbar = true)
                        is CreateUpdateOrder.OrderUpdateStatus.Succeeded -> {
                            viewState = viewState.copy(
                                isUpdatingOrderDraft = false,
                                showOrderUpdateSnackbar = false,
                                isEditable = updateStatus.order.isEditable,
                                multipleLinesContext = determineMultipleLinesContext(updateStatus.order)
                            )
                            _orderDraft.update { currentDraft ->
                                // Keep the user's selected status
                                updateStatus.order.copy(status = currentDraft.status)
                            }
                        }
                    }
                }
            _orderDraft.collect{
                Log.d("Order Id", it.id.toString())
            }
        }
    }

    override fun trackOrderSaveFailure(it: Throwable) {
        TODO("Not yet implemented")
    }

    override fun trackSaveOrderButtonClick() {
        TODO("Not yet implemented")
    }
}
