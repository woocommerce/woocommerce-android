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
    private val _recordTransactionDetailsChecked = MutableStateFlow(false)
    val recordTransactionDetailsChecked: StateFlow<Boolean> = _recordTransactionDetailsChecked

    sealed class UiState {
        data object Loading : UiState()
        data class Success(
            val amountDue: BigDecimal,
            val change: BigDecimal,
            val amountReceived: BigDecimal
        ) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    init {
        loadOrderDetails()
    }

    private fun loadOrderDetails() {
        launch {
            val order = orderDetailRepository.getOrderById(orderId)!!
            _uiState.value = UiState.Success(
                amountDue = order.total,
                change = BigDecimal.ZERO,
                amountReceived = BigDecimal.ZERO
            )
        }
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun updateAmountReceived(amount: BigDecimal) {
        val currentState = _uiState.value
        if (currentState is UiState.Success) {
            val newChange = amount - currentState.amountDue
            _uiState.value = currentState.copy(amountReceived = amount, change = newChange)
        }
    }

    private fun getCurrencySymbol(): String {
        val siteParameters = parameterRepository.getParameters()
        return siteParameters.currencySymbol.orEmpty()
    }

    fun updateRecordTransactionDetailsChecked(checked: Boolean) {
        _recordTransactionDetailsChecked.value = checked
    }

    fun addOrderNoteIfChecked() {
        if (recordTransactionDetailsChecked.value) {
            launch {
                val noteStringTemplate = resourceProvider.getString(R.string.cash_payments_order_note_text)
                val noteString = generateOrderNoteString(noteStringTemplate)
                val draftNote = OrderNote(note = noteString, isCustomerNote = false)

                _uiState.value = UiState.Loading

                orderDetailRepository.addOrderNote(orderId, draftNote)
                    .fold(
                        onSuccess = {
                            AnalyticsTracker.track(AnalyticsEvent.ORDER_NOTE_ADD_SUCCESS)
                            triggerEvent(MultiLiveEvent.Event.ExitWithResult(true))
                        },
                        onFailure = {
                            AnalyticsTracker.track(AnalyticsEvent.ORDER_NOTE_ADD_FAILED)
                            triggerEvent(MultiLiveEvent.Event.ExitWithResult(true))
                            triggerEvent(
                                MultiLiveEvent.Event.ShowSnackbar(R.string.cash_payments_order_note_adding_error)
                            )
                        },
                    )
            }
        } else {
            triggerEvent(MultiLiveEvent.Event.ExitWithResult(true))
        }
    }

    private fun generateOrderNoteString(noteStringTemplate: String): String {
        return when (val state = _uiState.value) {
            is UiState.Success -> {
                val currencySymbol = getCurrencySymbol()
                String.format(
                    noteStringTemplate,
                    "$currencySymbol${state.amountReceived.toPlainString()}",
                    "$currencySymbol${state.change.toPlainString()}"
                )
            }
            else -> noteStringTemplate
        }
    }
}
