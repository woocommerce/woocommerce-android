package com.woocommerce.android.ui.customfields.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.customfields.CustomField
import com.woocommerce.android.ui.customfields.CustomFieldUiModel
import com.woocommerce.android.ui.customfields.CustomFieldsRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomFieldsEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CustomFieldsRepository
) : ScopedViewModel(savedStateHandle) {
    private val navArgs by savedStateHandle.navArgs<CustomFieldsEditorFragmentArgs>()

    private val customFieldDraft = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = CustomFieldUiModel("", ""),
        key = "customField"
    )
    private val storedValue = MutableStateFlow<CustomField?>(null)
    private val isHtml = storedValue.map { it?.valueStrippedHtml != it?.valueAsString }

    val state = combine(
        customFieldDraft,
        storedValue,
        isHtml
    ) { customField, storedValue, isHtml ->
        UiState(
            customField = customField,
            showDoneButton = if (storedValue == null) {
                customField.key.isNotBlank() && customField.value.isNotBlank()
            } else {
                customField.key != storedValue.key || customField.value != storedValue.valueAsString
            },
            isHtml = isHtml
        )
    }

    init {
        loadStoreValue()
    }

    fun onKeyChanged(key: String) {
        customFieldDraft.update { it.copy(key = key) }
    }

    fun onValueChanged(value: String) {
        customFieldDraft.update { it.copy(value = value) }
    }

    fun onDoneClicked() {
        TODO()
    }

    private fun loadStoreValue() = launch {
        navArgs.customFieldId.takeIf { it != -1L }?.let {
            repository.getCustomFieldById(
                parentItemId = navArgs.parentItemId,
                customFieldId = it
            )
        }?.let {
            storedValue.value = it
        }
    }

    data class UiState(
        val customField: CustomFieldUiModel = CustomFieldUiModel("", ""),
        val showDoneButton: Boolean = false,
        val isHtml: Boolean = false
    )
}
