package com.woocommerce.android.ui.customfields.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.customfields.CustomFieldUiModel
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class CustomFieldsEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val navArgs by savedStateHandle.navArgs<CustomFieldsEditorFragmentArgs>()

    private val customFieldDraft = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = navArgs.customField ?: CustomFieldUiModel("", ""),
        key = "customField"
    )
    private val showDiscardChangesDialog = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = false,
        key = "showDiscardChangesDialog"
    )
    private val storedValue = navArgs.customField
    private val isHtml = storedValue?.valueStrippedHtml != storedValue?.value

    val state = combine(
        customFieldDraft,
        showDiscardChangesDialog.mapToState()
    ) { customField, discardChangesDialogState ->
        UiState(
            customField = customField,
            hasChanges = storedValue?.key.orEmpty() != customField.key ||
                storedValue?.value.orEmpty() != customField.value,
            isHtml = isHtml,
            discardChangesDialogState = discardChangesDialogState
        )
    }.asLiveData()

    fun onKeyChanged(key: String) {
        customFieldDraft.update { it.copy(key = key) }
    }

    fun onValueChanged(value: String) {
        customFieldDraft.update { it.copy(value = value) }
    }

    fun onDoneClicked() {
        val value = requireNotNull(customFieldDraft.value)
        triggerEvent(MultiLiveEvent.Event.ExitWithResult(value))
    }

    fun onBackClick() {
        if (state.value?.hasChanges == true) {
            showDiscardChangesDialog.value = true
        } else {
            triggerEvent(MultiLiveEvent.Event.Exit)
        }
    }

    private fun Flow<Boolean>.mapToState() = map {
        if (it) {
            DiscardChangesDialogState(
                onDiscard = { triggerEvent(MultiLiveEvent.Event.Exit) },
                onCancel = { showDiscardChangesDialog.value = false }
            )
        } else {
            null
        }
    }

    data class UiState(
        val customField: CustomFieldUiModel = CustomFieldUiModel("", ""),
        val hasChanges: Boolean = false,
        val isHtml: Boolean = false,
        val discardChangesDialogState: DiscardChangesDialogState? = null
    ) {
        val showDoneButton
            get() = customField.key.isNotEmpty() && hasChanges
    }

    data class DiscardChangesDialogState(
        val onDiscard: () -> Unit,
        val onCancel: () -> Unit
    )
}
