package com.woocommerce.android.ui.customfields.list

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.customfields.CustomFieldUiModel
import com.woocommerce.android.ui.customfields.CustomFieldsRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.metadata.UpdateMetadataRequest
import javax.inject.Inject

@HiltViewModel
class CustomFieldsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CustomFieldsRepository
) : ScopedViewModel(savedStateHandle) {
    private val args: CustomFieldsFragmentArgs by savedStateHandle.navArgs()

    private val isRefreshing = MutableStateFlow(false)
    private val isSaving = MutableStateFlow(false)
    private val showDiscardChangesDialog = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = false,
        key = "showDiscardChangesDialog"
    )
    private val customFields = repository.observeDisplayableCustomFields(args.parentItemId)
    private val pendingChanges = savedStateHandle.getStateFlow(viewModelScope, PendingChanges())

    val state = combine(
        customFields,
        pendingChanges,
        isRefreshing,
        isSaving,
        showDiscardChangesDialog
    ) { customFields, pendingChanges, isLoading, isSaving, isShowingDiscardDialog ->
        UiState(
            customFields = customFields.map { CustomFieldUiModel(it) }.combineWithChanges(pendingChanges),
            isRefreshing = isLoading,
            isSaving = isSaving,
            hasChanges = pendingChanges.hasChanges,
            discardChangesDialogState = isShowingDiscardDialog.takeIf { it }?.let {
                DiscardChangesDialogState(
                    onDiscard = { triggerEvent(MultiLiveEvent.Event.Exit) },
                    onCancel = { showDiscardChangesDialog.value = false }
                )
            }
        )
    }.asLiveData()

    fun onBackClick() {
        if (pendingChanges.value.hasChanges) {
            showDiscardChangesDialog.value = true
        } else {
            triggerEvent(MultiLiveEvent.Event.Exit)
        }
    }

    fun onPullToRefresh() {
        launch {
            isRefreshing.value = true
            repository.refreshCustomFields(args.parentItemId, args.parentItemType).onFailure {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.custom_fields_list_loading_error))
            }
            isRefreshing.value = false
        }
    }

    fun onCustomFieldClicked(field: CustomFieldUiModel) {
        triggerEvent(OpenCustomFieldEditor(field))
    }

    fun onCustomFieldValueClicked(field: CustomFieldUiModel) {
        triggerEvent(CustomFieldValueClicked(field))
    }

    fun onCustomFieldUpdated(result: CustomFieldUiModel) {
        pendingChanges.update {
            if (result.id == null) {
                it.copy(insertedFields = it.insertedFields + result)
            } else {
                it.copy(editedFields = it.editedFields + result)
            }
        }
    }

    fun onSaveClicked() {
        launch {
            isSaving.value = true
            val currentPendingChanges = pendingChanges.value
            val request = UpdateMetadataRequest(
                parentItemId = args.parentItemId,
                parentItemType = args.parentItemType,
                updatedMetadata = currentPendingChanges.editedFields.map { it.toDomainModel() },
                insertedMetadata = currentPendingChanges.insertedFields.map { it.toDomainModel() }
            )

            repository.updateCustomFields(request)
                .fold(
                    onSuccess = {
                        pendingChanges.value = PendingChanges()
                        triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.custom_fields_list_saving_succeeded))
                    },
                    onFailure = {
                        triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.custom_fields_list_saving_failed))
                    }
                )
            isSaving.value = false
        }
    }

    private fun List<CustomFieldUiModel>.combineWithChanges(pendingChanges: PendingChanges) = map { customField ->
        pendingChanges.editedFields.find { it.id == customField.id } ?: customField
    } + pendingChanges.insertedFields

    data class UiState(
        val customFields: List<CustomFieldUiModel>,
        val isRefreshing: Boolean = false,
        val isSaving: Boolean = false,
        val hasChanges: Boolean = false,
        val discardChangesDialogState: DiscardChangesDialogState? = null
    )

    data class DiscardChangesDialogState(
        val onDiscard: () -> Unit,
        val onCancel: () -> Unit
    )

    @Parcelize
    private data class PendingChanges(
        val editedFields: List<CustomFieldUiModel> = emptyList(),
        val insertedFields: List<CustomFieldUiModel> = emptyList()
    ) : Parcelable {
        val hasChanges: Boolean
            get() = editedFields.isNotEmpty() || insertedFields.isNotEmpty()
    }

    data class OpenCustomFieldEditor(val field: CustomFieldUiModel) : MultiLiveEvent.Event()
    data class CustomFieldValueClicked(val field: CustomFieldUiModel) : MultiLiveEvent.Event()
}
