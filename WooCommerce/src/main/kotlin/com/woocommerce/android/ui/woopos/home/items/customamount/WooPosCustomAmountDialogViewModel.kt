package com.woocommerce.android.ui.woopos.home.items.customamount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState
import com.woocommerce.android.viewmodel.ResourceProvider
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
    private val resourceProvider: ResourceProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(WooPosCustomAmountDialogState())
    val state: StateFlow<WooPosCustomAmountDialogState> = _state.asStateFlow()

    private var hasInitializedFor: WooPosCustomAmountDialogState.Mode? = null

    init {
        viewModelScope.launch {
            val params = getCurrencyFormattingParameters()
            _state.value = _state.value.copy(
                currencySymbol = params.currencySymbol,
                currencyPosition = params.currencyPosition,
                decimalSeparator = params.decimalSeparator,
                numberOfDecimals = params.numberOfDecimals,
            )
        }
    }

    fun initializeFor(editing: WooPosCartItemViewState.CustomAmount?) {
        val mode = editing
            ?.let { WooPosCustomAmountDialogState.Mode.Edit(it.itemNumber) }
            ?: WooPosCustomAmountDialogState.Mode.Add
        if (hasInitializedFor == mode) return
        hasInitializedFor = mode

        _state.value = _state.value.copy(
            mode = mode,
            amount = editing?.amount,
            name = editing?.name.orEmpty(),
            isTaxable = editing?.isTaxable ?: false,
            isSubmitting = false,
        )
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
        val editingItemNumber = (current.mode as? WooPosCustomAmountDialogState.Mode.Edit)?.itemNumber

        _state.value = current.copy(isSubmitting = true)
        viewModelScope.launch {
            try {
                childrenToParentEventSender.sendToParent(
                    ChildToParentEvent.CustomAmountSubmitted(
                        name = current.name.trim().ifBlank { defaultName() },
                        amount = amount,
                        isTaxable = current.isTaxable,
                        editingItemNumber = editingItemNumber,
                    )
                )
                hasInitializedFor = null
            } finally {
                _state.value = _state.value.copy(isSubmitting = false)
            }
        }
    }

    fun onDismissed() {
        hasInitializedFor = null
    }

    private fun defaultName(): String =
        resourceProvider.getString(R.string.woopos_custom_amount_dialog_name_placeholder)
}
