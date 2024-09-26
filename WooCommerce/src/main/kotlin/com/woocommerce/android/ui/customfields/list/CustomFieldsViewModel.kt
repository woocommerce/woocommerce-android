package com.woocommerce.android.ui.customfields.list

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.ui.customfields.CustomFieldUiModel
import com.woocommerce.android.ui.customfields.CustomFieldsRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.metadata.UpdateMetadataRequest
import javax.inject.Inject

@HiltViewModel
class CustomFieldsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CustomFieldsRepository,
    private val appPrefs: AppPrefsWrapper,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedStateHandle) {
    private val args: CustomFieldsFragmentArgs by savedStateHandle.navArgs()
    val parentItemId: Long = args.parentItemId

    private val isRefreshing = MutableStateFlow(false)
    private val isSaving = MutableStateFlow(false)
    private val customFields = repository.observeDisplayableCustomFields(args.parentItemId)
        .shareIn(viewModelScope, started = SharingStarted.Lazily)

    private val showDiscardChangesDialog = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = false,
        key = "showDiscardChangesDialog"
    )
    private val pendingChanges = savedStateHandle.getStateFlow(viewModelScope, PendingChanges())
    private val overlayedFieldId = savedStateHandle.getNullableStateFlow(
        viewModelScope, null, Long::class.java, "overlayedFieldId"
    )

    private val bannerDismissed = appPrefs.observePrefs()
        .onStart { emit(Unit) }
        .map { appPrefs.isCustomFieldsTopBannerDismissed }
        .distinctUntilChanged()

    private val customFieldsWithChanges = combine(
        customFields,
        pendingChanges
    ) { customFields, pendingChanges ->
        Pair(customFields.map { CustomFieldUiModel(it) }.combineWithChanges(pendingChanges), pendingChanges)
    }

    val state = combine(
        customFieldsWithChanges,
        isRefreshing,
        isSaving,
        showDiscardChangesDialog,
        bannerDismissed
    ) { (customFields, pendingChanges), isLoading, isSaving, isShowingDiscardDialog, bannerDismissed ->
        UiState(
            customFields = customFields,
            isRefreshing = isLoading,
            isSaving = isSaving,
            hasChanges = pendingChanges.hasChanges,
            discardChangesDialogState = isShowingDiscardDialog.takeIf { it }?.let {
                DiscardChangesDialogState(
                    onDiscard = { triggerEvent(MultiLiveEvent.Event.Exit) },
                    onCancel = { showDiscardChangesDialog.value = false }
                )
            },
            topBannerState = bannerDismissed.takeIf { !it }?.let {
                TopBannerState {
                    appPrefs.isCustomFieldsTopBannerDismissed = true
                }
            }
        )
    }.asLiveData()

    val overlayedField = combine(
        customFieldsWithChanges,
        overlayedFieldId
    ) { (customFields, _), fieldId ->
        customFields.find { it.id == fieldId }
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
        if (field.isJson) {
            overlayedFieldId.value = field.id
        } else {
            triggerEvent(OpenCustomFieldEditor(field))
        }
    }

    fun onOverlayedFieldDismissed() {
        overlayedFieldId.value = null
    }

    fun onCustomFieldValueClicked(field: CustomFieldUiModel) {
        triggerEvent(CustomFieldValueClicked(field))
    }

    fun onAddCustomFieldClicked() {
        triggerEvent(OpenCustomFieldEditor(null))
    }

    fun onCustomFieldInserted(result: CustomFieldUiModel) {
        pendingChanges.update {
            it.copy(insertedFields = it.insertedFields + result)
        }
    }

    fun onCustomFieldUpdated(oldValueKey: String, result: CustomFieldUiModel) {
        pendingChanges.update {
            if (result.id == null) {
                // We are updating a field that was just added and hasn't been saved yet
                it.copy(insertedFields = it.insertedFields.filterNot { field -> field.key == oldValueKey } + result)
            } else {
                it.copy(editedFields = it.editedFields.filterNot { field -> field.id == result.id } + result)
            }
        }
    }

    fun onCustomFieldDeleted(field: CustomFieldUiModel) {
        pendingChanges.update {
            if (field.id == null) {
                // This field was just added and hasn't been saved yet
                it.copy(insertedFields = it.insertedFields - field)
            } else {
                it.copy(deletedFieldIds = it.deletedFieldIds + field.id)
            }
        }

        triggerEvent(
            MultiLiveEvent.Event.ShowActionSnackbar(
                message = resourceProvider.getString(R.string.custom_fields_list_field_deleted),
                actionText = resourceProvider.getString(R.string.undo),
                action = {
                    pendingChanges.update {
                        if (field.id == null) {
                            it.copy(insertedFields = it.insertedFields + field)
                        } else {
                            it.copy(deletedFieldIds = it.deletedFieldIds - field.id)
                        }
                    }
                }
            )
        )
    }

    fun onSaveClicked() {
        launch {
            isSaving.value = true
            val currentPendingChanges = pendingChanges.value
            val request = UpdateMetadataRequest(
                parentItemId = args.parentItemId,
                parentItemType = args.parentItemType,
                updatedMetadata = currentPendingChanges.editedFields.map { it.toDomainModel() },
                insertedMetadata = currentPendingChanges.insertedFields.map { it.toDomainModel() },
                deletedMetadataIds = currentPendingChanges.deletedFieldIds
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

    private fun List<CustomFieldUiModel>.combineWithChanges(pendingChanges: PendingChanges) =
        filterNot { it.id in pendingChanges.deletedFieldIds }
            .map { customField ->
                pendingChanges.editedFields.find { it.id == customField.id } ?: customField
            }
            .plus(pendingChanges.insertedFields)

    data class UiState(
        val customFields: List<CustomFieldUiModel>,
        val isRefreshing: Boolean = false,
        val isSaving: Boolean = false,
        val hasChanges: Boolean = false,
        val discardChangesDialogState: DiscardChangesDialogState? = null,
        val topBannerState: TopBannerState? = null
    )

    data class DiscardChangesDialogState(
        val onDiscard: () -> Unit,
        val onCancel: () -> Unit
    )

    data class TopBannerState(
        val onDismiss: () -> Unit
    )

    @Parcelize
    private data class PendingChanges(
        val editedFields: List<CustomFieldUiModel> = emptyList(),
        val insertedFields: List<CustomFieldUiModel> = emptyList(),
        val deletedFieldIds: List<Long> = emptyList()
    ) : Parcelable {
        val hasChanges: Boolean
            get() = editedFields.isNotEmpty() || insertedFields.isNotEmpty() || deletedFieldIds.isNotEmpty()
    }

    data class OpenCustomFieldEditor(val field: CustomFieldUiModel?) : MultiLiveEvent.Event()
    data class CustomFieldValueClicked(val field: CustomFieldUiModel) : MultiLiveEvent.Event()
}
