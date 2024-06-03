package com.woocommerce.android.ui.payments.changeduecalculator

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class ChangeDueCalculatorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderDetailRepository: OrderDetailRepository,
    private val parameterRepository: ParameterRepository,
    private val resourceProvider: ResourceProvider,
) : ScopedViewModel(savedStateHandle) {
    val navArgs: ChangeDueCalculatorFragmentArgs by savedStateHandle.navArgs()
    private val orderId: Long = navArgs.orderId

    data class UiState(
        val amountDue: BigDecimal = BigDecimal.ZERO,
        val change: BigDecimal = BigDecimal.ZERO,
        val amountReceived: BigDecimal = BigDecimal.ZERO,
        val loading: Boolean = false,
        val recordTransactionDetailsChecked: Boolean = false,
        val canCompleteOrder: Boolean,
        val currencySymbol: String,
    )

    private val _uiState = MutableStateFlow(
        UiState(
            loading = true,
            currencySymbol = getCurrencySymbol(),
            canCompleteOrder = false,
        )
    )
    val uiState: StateFlow<UiState> = _uiState

    init {
        loadOrderDetails()
    }

    private fun loadOrderDetails() {
        launch {
            val order = orderDetailRepository.getOrderById(orderId)!!
            _uiState.value = UiState(
                amountDue = order.total,
                change = BigDecimal.ZERO,
                amountReceived = order.total,
                canCompleteOrder = true,
                currencySymbol = getCurrencySymbol(),
            )
        }
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun updateAmountReceived(amount: BigDecimal) {
        val currentState = _uiState.value
        val newChange = amount - currentState.amountDue
        _uiState.value = currentState.copy(
            amountReceived = amount,
            change = newChange,
            canCompleteOrder = newChange >= BigDecimal.ZERO,
        )
    }

    fun updateRecordTransactionDetailsChecked(checked: Boolean) {
        _uiState.value = _uiState.value.copy(recordTransactionDetailsChecked = checked)
    }

    fun onOrderComplete() {
        if (_uiState.value.recordTransactionDetailsChecked) {
            launch {
                val noteStringTemplate = resourceProvider.getString(R.string.cash_payments_order_note_text)
                val noteString = generateOrderNoteString(noteStringTemplate)
                val draftNote = OrderNote(note = noteString, isCustomerNote = false)

                _uiState.value = _uiState.value.copy(loading = true)

                orderDetailRepository.addOrderNote(orderId, draftNote)
                    .fold(
                        onSuccess = {
                            AnalyticsTracker.track(AnalyticsEvent.ORDER_NOTE_ADD_SUCCESS)
                            triggerEvent(MultiLiveEvent.Event.ExitWithResult(true))
                        },
                        onFailure = {
                            AnalyticsTracker.track(AnalyticsEvent.ORDER_NOTE_ADD_FAILED)
                            triggerEvent(
                                MultiLiveEvent.Event.ShowSnackbar(R.string.cash_payments_order_note_adding_error)
                            )
                            triggerEvent(MultiLiveEvent.Event.ExitWithResult(true))
                        },
                    )
            }
        } else {
            triggerEvent(MultiLiveEvent.Event.ExitWithResult(true))
        }
    }

    private fun getCurrencySymbol(): String {
        val siteParameters = parameterRepository.getParameters()
        return siteParameters.currencySymbol.orEmpty()
    }

    private fun generateOrderNoteString(noteStringTemplate: String): String {
        val state = _uiState.value
        val currencySymbol = getCurrencySymbol()
        return String.format(
            noteStringTemplate,
            "$currencySymbol${state.amountReceived.toPlainString()}",
            "$currencySymbol${state.change.toPlainString()}"
        )
    }
}
