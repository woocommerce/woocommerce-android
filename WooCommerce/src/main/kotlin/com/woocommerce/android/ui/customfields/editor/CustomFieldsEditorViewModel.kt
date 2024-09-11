package com.woocommerce.android.ui.customfields.editor

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.customfields.CustomFieldUiModel
import com.woocommerce.android.ui.customfields.CustomFieldsRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
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
    companion object {
        const val CUSTOM_FIELD_CREATED_RESULT_KEY = "custom_field_created"
        const val CUSTOM_FIELD_UPDATED_RESULT_KEY = "custom_field_updated"
        const val CUSTOM_FIELD_DELETED_RESULT_KEY = "custom_field_deleted"
    }

    private val navArgs by savedStateHandle.navArgs<CustomFieldsEditorFragmentArgs>()

    private val customFieldDraft = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = navArgs.customField ?: CustomFieldUiModel("", ""),
        key = "customFieldDraft"
    )
    private val showDiscardChangesDialog = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = false,
        key = "showDiscardChangesDialog"
    )
    private val keyErrorMessage = savedStateHandle.getNullableStateFlow(
        scope = viewModelScope,
        initialValue = null,
        clazz = UiString::class.java,
        key = "keyErrorMessage"
    )
    private val storedValue = navArgs.customField
    private val isHtml = storedValue?.valueStrippedHtml != storedValue?.value

    val state = combine(
        customFieldDraft,
        showDiscardChangesDialog.mapToState(),
        keyErrorMessage
    ) { customField, discardChangesDialogState, keyErrorMessage ->
        UiState(
            customField = customField,
            hasChanges = storedValue?.key.orEmpty() != customField.key ||
                storedValue?.value.orEmpty() != customField.value,
            isHtml = isHtml,
            discardChangesDialogState = discardChangesDialogState,
            keyErrorMessage = keyErrorMessage,
            isCreatingNewItem = storedValue == null
        )
    }.asLiveData()

    fun onKeyChanged(key: String) {
        keyErrorMessage.value = if (key.startsWith("_")) {
            UiString.UiStringRes(R.string.custom_fields_editor_key_error_underscore)
        } else {
            null
        }
        customFieldDraft.update { it.copy(key = key) }
    }

    fun onValueChanged(value: String) {
        customFieldDraft.update { it.copy(value = value) }
    }

    fun onDoneClicked() {
        launch {
            val value = requireNotNull(customFieldDraft.value)
            if (value.id == null) {
                // Check for duplicate keys before inserting the new custom field
                // For more context: pe5sF9-33t-p2#comment-3880
                val existingFields = repository.getDisplayableCustomFields(navArgs.parentItemId)
                if (existingFields.any { it.key == value.key }) {
                    keyErrorMessage.value = UiString.UiStringRes(R.string.custom_fields_editor_key_error_duplicate)
                    return@launch
                }
            }

            val event = if (storedValue == null) {
                MultiLiveEvent.Event.ExitWithResult(data = value, key = CUSTOM_FIELD_CREATED_RESULT_KEY)

            } else {
                MultiLiveEvent.Event.ExitWithResult(data = value, key = CUSTOM_FIELD_UPDATED_RESULT_KEY)
            }
            triggerEvent(event)
        }
    }

    fun onDeleteClicked() {
        triggerEvent(
            MultiLiveEvent.Event.ExitWithResult(data = navArgs.customField, key = CUSTOM_FIELD_DELETED_RESULT_KEY)
        )
    }

    fun onCopyKeyClicked() {
        triggerEvent(CopyContentToClipboard(R.string.custom_fields_editor_key_label, customFieldDraft.value.key))
    }

    fun onCopyValueClicked() {
        triggerEvent(CopyContentToClipboard(R.string.custom_fields_editor_value_label, customFieldDraft.value.value))
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
        val discardChangesDialogState: DiscardChangesDialogState? = null,
        val keyErrorMessage: UiString? = null,
        val isCreatingNewItem: Boolean = false
    ) {
        val showDoneButton
            get() = customField.key.isNotEmpty() && hasChanges && keyErrorMessage == null
    }

    data class DiscardChangesDialogState(
        val onDiscard: () -> Unit,
        val onCancel: () -> Unit
    )

    data class CopyContentToClipboard(
        @StringRes val labelResource: Int,
        val content: String
    ) : MultiLiveEvent.Event()
}
