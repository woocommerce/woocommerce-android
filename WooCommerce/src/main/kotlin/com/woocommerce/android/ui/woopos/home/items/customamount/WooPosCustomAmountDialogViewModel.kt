package com.woocommerce.android.ui.woopos.home.items.customamount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class WooPosCustomAmountDialogViewModel @Inject constructor(
    private val getCurrencyFormattingParameters: WooPosGetCurrencyFormattingParameters,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
) : ViewModel() {

    private val _state = MutableStateFlow(WooPosCustomAmountDialogState())
    val state: StateFlow<WooPosCustomAmountDialogState> = _state.asStateFlow()

    private var lastInitializedFor: Any? = NOT_INITIALIZED

    fun initializeFor(editing: WooPosCartItemViewState.CustomAmount?) {
        val key: Any = editing?.itemNumber ?: ADD_MODE_KEY
        if (lastInitializedFor == key) return
        lastInitializedFor = key

        viewModelScope.launch {
            val params = getCurrencyFormattingParameters()
            _state.value = if (editing != null) {
                WooPosCustomAmountDialogState(
                    mode = WooPosCustomAmountDialogState.Mode.Edit(
                        itemNumber = editing.itemNumber,
                        customAmountId = editing.customAmountId,
                    ),
                    amount = editing.amount,
                    name = editing.name,
                    isTaxable = editing.isTaxable,
                    currencySymbol = params.currencySymbol,
                    currencyPosition = params.currencyPosition,
                    decimalSeparator = params.decimalSeparator,
                    numberOfDecimals = params.numberOfDecimals,
                )
            } else {
                WooPosCustomAmountDialogState(
                    mode = WooPosCustomAmountDialogState.Mode.Add,
                    currencySymbol = params.currencySymbol,
                    currencyPosition = params.currencyPosition,
                    decimalSeparator = params.decimalSeparator,
                    numberOfDecimals = params.numberOfDecimals,
                )
            }
        }
    }

    fun onAmountChanged(value: BigDecimal?) {
        _state.value = _state.value.copy(amount = value)
    }

    fun onNameChanged(value: String) {
        _state.value = _state.value.copy(name = value)
    }

    fun onTaxableToggled(value: Boolean) {
        _state.value = _state.value.copy(isTaxable = value)
    }

    fun onSubmit() {
        val current = _state.value
        if (!current.isSubmitEnabled) return
        val amount = current.amount ?: return
        val mode = current.mode

        val customAmountId: Long
        val editingItemNumber: Int?
        when (mode) {
            is WooPosCustomAmountDialogState.Mode.Edit -> {
                customAmountId = mode.customAmountId
                editingItemNumber = mode.itemNumber
            }
            WooPosCustomAmountDialogState.Mode.Add -> {
                customAmountId = System.currentTimeMillis()
                editingItemNumber = null
            }
        }

        viewModelScope.launch {
            childrenToParentEventSender.sendToParent(
                ChildToParentEvent.CustomAmountSubmitted(
                    customAmountId = customAmountId,
                    name = current.name.trim().ifBlank { DEFAULT_CUSTOM_AMOUNT_NAME },
                    amount = amount,
                    isTaxable = current.isTaxable,
                    editingItemNumber = editingItemNumber,
                )
            )
        }
        lastInitializedFor = NOT_INITIALIZED
    }

    fun onDismissed() {
        lastInitializedFor = NOT_INITIALIZED
    }

    private companion object {
        const val DEFAULT_CUSTOM_AMOUNT_NAME = "Custom amount"
        const val ADD_MODE_KEY = "add"
        val NOT_INITIALIZED = Any()
    }
}
