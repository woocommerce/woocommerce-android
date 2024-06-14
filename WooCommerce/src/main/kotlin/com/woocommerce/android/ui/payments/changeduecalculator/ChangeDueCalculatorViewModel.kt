package com.woocommerce.android.ui.payments.changeduecalculator

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.WCSettingsModel
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class ChangeDueCalculatorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderDetailRepository: OrderDetailRepository,
    private val parameterRepository: ParameterRepository,
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter
) : ScopedViewModel(savedStateHandle) {
    val navArgs: ChangeDueCalculatorFragmentArgs by savedStateHandle.navArgs()
    private val orderId: Long = navArgs.orderId
    private val siteParameters = parameterRepository.getParameters()

    data class UiState(
        val amountDue: BigDecimal = BigDecimal.ZERO,
        val change: BigDecimal = BigDecimal.ZERO,
        val amountReceived: BigDecimal = BigDecimal.ZERO,
        val loading: Boolean = false,
        val recordTransactionDetailsChecked: Boolean = false,
        val canCompleteOrder: Boolean,
        val currencySymbol: String,
        val currencyPosition: WCSettingsModel.CurrencyPosition,
        val decimalSeparator: String,
        val numberOfDecimals: Int,
        val title: String = "",
        val changeDueText: String = ""
    )

    private val _uiState = MutableStateFlow(
        UiState(
            loading = true,
            canCompleteOrder = false,
            currencySymbol = siteParameters.currencySymbol.orEmpty(),
            currencyPosition = getCurrencySymbolPosition(),
            decimalSeparator = getDecimalSeparator(),
            numberOfDecimals = getNumberOfDecimals(),
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
                currencySymbol = siteParameters.currencySymbol.orEmpty(),
                currencyPosition = getCurrencySymbolPosition(),
                decimalSeparator = getDecimalSeparator(),
                numberOfDecimals = getNumberOfDecimals(),
                title = getTitleText(order.total),
                changeDueText = "-"
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
            changeDueText = getChangeDueText(newChange)
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

    private fun getCurrencySymbolPosition(): WCSettingsModel.CurrencyPosition {
        val siteParameters = parameterRepository.getParameters()
        var position = WCSettingsModel.CurrencyPosition.LEFT
        if (siteParameters.currencyFormattingParameters != null) {
            position = siteParameters.currencyFormattingParameters.currencyPosition
        }
        return position
    }

    private fun getDecimalSeparator(): String {
        if (siteParameters.currencyFormattingParameters == null) {
            return "."
        }
        return siteParameters.currencyFormattingParameters.currencyDecimalSeparator
    }

    private fun getNumberOfDecimals(): Int {
        if (siteParameters.currencyFormattingParameters == null) {
            return 2
        }
        return siteParameters.currencyFormattingParameters.currencyDecimalNumber
    }

    private fun generateOrderNoteString(noteStringTemplate: String): String {
        val state = _uiState.value
        val currencySymbol = siteParameters.currencySymbol.orEmpty()
        return String.format(
            noteStringTemplate,
            "$currencySymbol${state.amountReceived.toPlainString()}",
            "$currencySymbol${state.change.toPlainString()}"
        )
    }

    private fun getTitleText(total: BigDecimal): String {
        return resourceProvider.getString(
            R.string.cash_payments_take_payment_title,
            currencyFormatter.formatCurrency(total)
        )
    }

    private fun getChangeDueText(newChange: BigDecimal): String {
        return if ((newChange < BigDecimal.ZERO)) {
            "-"
        } else {
            currencyFormatter.formatCurrency(newChange)
        }
    }
}
