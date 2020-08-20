package com.woocommerce.android.ui.products.downloads

import android.content.DialogInterface
import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ProductFile
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
            fileDraft = navArgs.productFile ?: ProductFile(null, "", ""),
            hasChanges = false
        )
    )
    private var productDownloadDetailsViewState by productDownloadDetailsViewStateData

    val hasChanges
        get() = productDownloadDetailsViewState.hasChanges

    val screenTitle
        get() = navArgs.productFile?.name
            ?.ifEmpty { resourceProvider.getString(R.string.product_downloadable_files_edit_title) }
            ?: TODO("Should be implemented for files creation")

    fun onFileUrlChanged(url: String) {
        val updatedDraft = productDownloadDetailsViewState.fileDraft.copy(url = url)
        updateState(productDownloadDetailsViewState.copy(fileDraft = updatedDraft))
    }

    fun onFileNameChanged(name: String) {
        val updatedDraft = productDownloadDetailsViewState.fileDraft.copy(name = name)
        updateState(productDownloadDetailsViewState.copy(fileDraft = updatedDraft))
    }

    fun onDoneOrUpdateClicked() {
        // TODO handle file creation by checking if the navArgs file is null
        triggerEvent(UpdateFileAndExitEvent(productDownloadDetailsViewState.fileDraft))
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
        return if (hasChanges) {
            triggerEvent(ShowDialog.buildDiscardDialogEvent(
                positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                    triggerEvent(Exit)
                }
            ))
            false
        } else true
    }

    private fun updateState(updatedState: ProductDownloadDetailsViewState) {
        val hasChanges = updatedState.fileDraft != navArgs.productFile
        productDownloadDetailsViewState = updatedState.copy(hasChanges = hasChanges)
    }

    fun triggerFileDeletion() {
        triggerEvent(
            DeleteFileEvent(
                navArgs.productFile
                    ?: throw IllegalStateException("The delete action can't be invoked if the file to edit is null")
            )
        )
    }

    sealed class ProductDownloadDetailsEvent : Event() {
        data class UpdateFileAndExitEvent(
            val updatedFile: ProductFile
        ) : ProductDownloadDetailsEvent()

        class DeleteFileEvent(val file: ProductFile) : ProductDownloadDetailsEvent()
    }

    @Parcelize
    data class ProductDownloadDetailsViewState(
        val fileDraft: ProductFile,
        val hasChanges: Boolean
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductDownloadDetailsViewModel>
}
