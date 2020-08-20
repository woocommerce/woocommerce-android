package com.woocommerce.android.ui.products.downloads

import android.content.DialogInterface
import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ProductFile
import com.woocommerce.android.ui.products.downloads.ProductDownloadDetailsViewModel.ProductDownloadDetailsEvent.AddFileAndExitEvent
import com.woocommerce.android.ui.products.downloads.ProductDownloadDetailsViewModel.ProductDownloadDetailsEvent.DeleteFileEvent
import com.woocommerce.android.ui.products.downloads.ProductDownloadDetailsViewModel.ProductDownloadDetailsEvent.UpdateFileAndExitEvent
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize

class ProductDownloadDetailsViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedState, dispatchers) {
    private val navArgs: ProductDownloadDetailsFragmentArgs by savedState.navArgs()

    val productDownloadDetailsViewStateData = LiveDataDelegate(
        savedState,
        ProductDownloadDetailsViewState(
            fileDraft = navArgs.productFile,
            showDoneButton = !navArgs.isEditing
        )
    )
    private var productDownloadDetailsViewState by productDownloadDetailsViewStateData

    val showDoneButton
        get() = productDownloadDetailsViewState.showDoneButton

    val screenTitle
        get() = if (navArgs.isEditing) {
            navArgs.productFile.name
                .ifEmpty { resourceProvider.getString(R.string.product_downloadable_files_edit_title) }
        } else {
            resourceProvider.getString(R.string.product_downloadable_files_add_title)
        }

    fun onFileUrlChanged(url: String) {
        val updatedDraft = productDownloadDetailsViewState.fileDraft.copy(url = url)
        updateState(productDownloadDetailsViewState.copy(fileDraft = updatedDraft))
    }

    fun onFileNameChanged(name: String) {
        val updatedDraft = productDownloadDetailsViewState.fileDraft.copy(name = name)
        updateState(productDownloadDetailsViewState.copy(fileDraft = updatedDraft))
    }

    fun onDoneOrUpdateClicked() {
        if (navArgs.isEditing) {
            triggerEvent(UpdateFileAndExitEvent(productDownloadDetailsViewState.fileDraft))
        } else {
            triggerEvent(AddFileAndExitEvent(productDownloadDetailsViewState.fileDraft))
        }
    }

    fun onDeleteButtonClicked() {
        triggerEvent(
            ShowDialog(
                messageId = R.string.product_downloadable_files_delete_confirmation,
                positiveButtonId = R.string.delete,
                positiveBtnAction = DialogInterface.OnClickListener { _, _ -> triggerFileDeletion() },
                negativeButtonId = R.string.cancel
            )
        )
    }

    fun onBackButtonClicked(): Boolean {
        return if (showDoneButton) {
            triggerEvent(ShowDialog.buildDiscardDialogEvent(
                positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                    triggerEvent(Exit)
                }
            ))
            false
        } else true
    }

    private fun updateState(updatedState: ProductDownloadDetailsViewState) {
        val hasChanges = !navArgs.isEditing || updatedState.fileDraft != navArgs.productFile
        val canSave = updatedState.fileDraft.url.isNotBlank()
        productDownloadDetailsViewState = updatedState.copy(showDoneButton = hasChanges && canSave)
    }

    fun triggerFileDeletion() {
        triggerEvent(DeleteFileEvent(navArgs.productFile))
    }

    sealed class ProductDownloadDetailsEvent : Event() {
        data class UpdateFileAndExitEvent(
            val updatedFile: ProductFile
        ) : ProductDownloadDetailsEvent()

        data class AddFileAndExitEvent(
            val file: ProductFile
        ) : ProductDownloadDetailsEvent()

        class DeleteFileEvent(val file: ProductFile) : ProductDownloadDetailsEvent()
    }

    @Parcelize
    data class ProductDownloadDetailsViewState(
        val fileDraft: ProductFile,
        val showDoneButton: Boolean
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductDownloadDetailsViewModel>
}
