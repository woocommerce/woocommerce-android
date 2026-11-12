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
class WooPosCustomAmountFormViewModel @Inject constructor(
    private val getCurrencyFormattingParameters: WooPosGetCurrencyFormattingParameters,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val resourceProvider: ResourceProvider,
    private val taxableRepository: WooPosCustomAmountTaxableRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WooPosCustomAmountFormState())
    val state: StateFlow<WooPosCustomAmountFormState> = _state.asStateFlow()

    private var hasInitializedFor: WooPosCustomAmountFormState.Mode? = null

    private var amountBeforeEditing: BigDecimal? = null

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
        }
    }

    fun initializeFor(mode: WooPosCustomAmountFormState.Mode) {
        if (hasInitializedFor == mode) return
        hasInitializedFor = mode
        val editing = mode as? WooPosCustomAmountFormState.Mode.Edit
        amountBeforeEditing = null

        _state.value = _state.value.copy(
            mode = mode,
            amount = editing?.amount,
            // A blank name is saved as the default name, so show it as unset again when editing.
            name = editing?.name?.takeUnless { it == defaultName() }.orEmpty(),
            nameDraft = "",
            isTaxable = editing?.isTaxable ?: taxableRepository.getLastIsTaxable(),
            isSubmitting = false,
            isSubmitted = false,
            isClosed = false,
            screen = if (editing == null) {
                WooPosCustomAmountFormState.Screen.AMOUNT
            } else {
                WooPosCustomAmountFormState.Screen.DETAILS
            },
            returnsToDetails = false,
        )
    }

    private fun onAmountChanged(value: BigDecimal?) {
        _state.value = _state.value.copy(amount = value)
    }

    private fun onAmountConfirmed() {
        if (!_state.value.isAmountValid) return
        amountBeforeEditing = null
        _state.value = _state.value.copy(
            screen = WooPosCustomAmountFormState.Screen.DETAILS,
            returnsToDetails = false,
        )
    }

    private fun onEditAmount() {
        amountBeforeEditing = _state.value.amount
        _state.value = _state.value.copy(
            screen = WooPosCustomAmountFormState.Screen.AMOUNT,
            returnsToDetails = true,
        )
    }

    private fun onAmountBack() {
        if (_state.value.returnsToDetails) {
            _state.value = _state.value.copy(
                amount = amountBeforeEditing,
                screen = WooPosCustomAmountFormState.Screen.DETAILS,
                returnsToDetails = false,
            )
            amountBeforeEditing = null
        } else {
            _state.value = _state.value.copy(isClosed = true)
        }
    }

    private fun onNameChanged(value: String) {
        _state.value = _state.value.copy(name = value)
    }

    private fun onEditName() {
        _state.value = _state.value.copy(
            screen = WooPosCustomAmountFormState.Screen.NAME,
            nameDraft = _state.value.name,
        )
    }

    private fun onNameDraftChanged(value: String) {
        _state.value = _state.value.copy(nameDraft = value)
    }

    private fun onNameConfirmed() {
        _state.value = _state.value.copy(
            name = _state.value.nameDraft,
            screen = WooPosCustomAmountFormState.Screen.DETAILS,
        )
    }

    private fun onNameBack() {
        _state.value = _state.value.copy(nameDraft = "", screen = WooPosCustomAmountFormState.Screen.DETAILS)
    }

    private fun onTaxableToggled(value: Boolean) {
        _state.value = _state.value.copy(isTaxable = value)
    }

    private fun onSubmit() {
        val current = _state.value
        if (!current.isSubmitEnabled) return
        val amount = current.amount ?: return
        val editingItemNumber = (current.mode as? WooPosCustomAmountFormState.Mode.Edit)?.itemNumber

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
                taxableRepository.saveLastIsTaxable(current.isTaxable)
                hasInitializedFor = null
                _state.value = _state.value.copy(isSubmitted = true)
            } finally {
                _state.value = _state.value.copy(isSubmitting = false)
            }
        }
    }

    private fun defaultName(): String =
        resourceProvider.getString(R.string.woopos_custom_amount_dialog_name_placeholder)
}
