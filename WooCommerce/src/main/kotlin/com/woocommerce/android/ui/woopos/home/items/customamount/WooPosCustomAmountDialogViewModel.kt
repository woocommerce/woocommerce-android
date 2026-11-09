package com.woocommerce.android.ui.woopos.home.items.customamount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
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

    private var amountBeforeEditing: BigDecimal? = null
    private var returnToDetailsAfterAmount = false

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

    fun onUIEvent(event: WooPosCustomAmountUIEvent) {
        when (event) {
            is WooPosCustomAmountUIEvent.AmountChanged -> onAmountChanged(event.amount)
            is WooPosCustomAmountUIEvent.NameChanged -> onNameChanged(event.name)
            is WooPosCustomAmountUIEvent.TaxableToggled -> onTaxableToggled(event.isTaxable)
            WooPosCustomAmountUIEvent.AmountConfirmed -> onAmountConfirmed()
            WooPosCustomAmountUIEvent.EditAmountClicked -> onEditAmount()
            WooPosCustomAmountUIEvent.AmountBackClicked -> onAmountBack()
            WooPosCustomAmountUIEvent.EditNameClicked -> onEditName()
            is WooPosCustomAmountUIEvent.NameDraftChanged -> onNameDraftChanged(event.name)
            WooPosCustomAmountUIEvent.NameConfirmed -> onNameConfirmed()
            WooPosCustomAmountUIEvent.NameBackClicked -> onNameBack()
            WooPosCustomAmountUIEvent.SubmitClicked -> onSubmit()
            WooPosCustomAmountUIEvent.Closed -> onDismissed()
        }
    }

    fun initializeFor(mode: WooPosCustomAmountDialogState.Mode) {
        if (hasInitializedFor == mode) return
        hasInitializedFor = mode
        val editing = mode as? WooPosCustomAmountDialogState.Mode.Edit
        amountBeforeEditing = null
        returnToDetailsAfterAmount = false

        _state.value = _state.value.copy(
            mode = mode,
            amount = editing?.amount,
            // A blank name is saved as the default name, so show it as unset again when editing.
            name = editing?.name?.takeUnless { it == defaultName() }.orEmpty(),
            nameDraft = "",
            isTaxable = editing?.isTaxable ?: false,
            isSubmitting = false,
            isSubmitted = false,
            isClosed = false,
            screen = if (editing == null) {
                WooPosCustomAmountDialogState.Screen.AMOUNT
            } else {
                WooPosCustomAmountDialogState.Screen.DETAILS
            },
        )
    }

    private fun onAmountChanged(value: BigDecimal?) {
        _state.value = _state.value.copy(amount = value)
    }

    private fun onAmountConfirmed() {
        if (!_state.value.isAmountValid) return
        amountBeforeEditing = null
        returnToDetailsAfterAmount = false
        _state.value = _state.value.copy(screen = WooPosCustomAmountDialogState.Screen.DETAILS)
    }

    private fun onEditAmount() {
        amountBeforeEditing = _state.value.amount
        returnToDetailsAfterAmount = true
        _state.value = _state.value.copy(screen = WooPosCustomAmountDialogState.Screen.AMOUNT)
    }

    private fun onAmountBack() {
        if (returnToDetailsAfterAmount) {
            _state.value = _state.value.copy(
                amount = amountBeforeEditing,
                screen = WooPosCustomAmountDialogState.Screen.DETAILS,
            )
            amountBeforeEditing = null
            returnToDetailsAfterAmount = false
        } else {
            _state.value = _state.value.copy(isClosed = true)
        }
    }

    private fun onNameChanged(value: String) {
        _state.value = _state.value.copy(name = value)
    }

    private fun onEditName() {
        _state.value = _state.value.copy(
            screen = WooPosCustomAmountDialogState.Screen.NAME,
            nameDraft = _state.value.name,
        )
    }

    private fun onNameDraftChanged(value: String) {
        _state.value = _state.value.copy(nameDraft = value)
    }

    private fun onNameConfirmed() {
        _state.value = _state.value.copy(
            name = _state.value.nameDraft,
            screen = WooPosCustomAmountDialogState.Screen.DETAILS,
        )
    }

    private fun onNameBack() {
        _state.value = _state.value.copy(nameDraft = "", screen = WooPosCustomAmountDialogState.Screen.DETAILS)
    }

    private fun onTaxableToggled(value: Boolean) {
        _state.value = _state.value.copy(isTaxable = value)
    }

    private fun onSubmit() {
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
                _state.value = _state.value.copy(isSubmitted = true)
            } finally {
                _state.value = _state.value.copy(isSubmitting = false)
            }
        }
    }

    private fun onDismissed() {
        hasInitializedFor = null
        _state.value = _state.value.copy(isSubmitted = false)
    }

    private fun defaultName(): String =
        resourceProvider.getString(R.string.woopos_custom_amount_dialog_name_placeholder)
}
