package com.woocommerce.android.ui.products.downloads

import android.content.DialogInterface
import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ProductFile
import com.woocommerce.android.ui.products.downloads.ProductDownloadDetailsViewModel.ProductDownloadDetailsEvent.UpdateFileAndExitEvent
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize

class ProductDownloadDetailsViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers
) : ScopedViewModel(savedState, dispatchers) {
    private val navArgs: ProductDownloadDetailsFragmentArgs by savedState.navArgs()

    val productDownloadDetailsViewStateData = LiveDataDelegate(
        savedState,
        ProductDownloadDetailsViewState(
            fileDraft = navArgs.productFile ?: ProductFile(null, "", ""),
            downloadLimit = navArgs.downloadLimit,
            downloadExpiry = navArgs.downloadExpiry,
            hasChanges = false
        )
    )
    private var productDownloadDetailsViewState by productDownloadDetailsViewStateData

    val hasChanges
        get() = productDownloadDetailsViewState.hasChanges

    val screenTitle
        get() = navArgs.productFile?.name ?: TODO("Should be implemented for files creation")

    fun onFileUrlChanged(url: String) {
        val updatedDraft = productDownloadDetailsViewState.fileDraft.copy(url = url)
        updateState(productDownloadDetailsViewState.copy(fileDraft = updatedDraft))
    }

    fun onFileNameChanged(name: String) {
        val updatedDraft = productDownloadDetailsViewState.fileDraft.copy(name = name)
        updateState(productDownloadDetailsViewState.copy(fileDraft = updatedDraft))
    }

    fun onDownloadExpiryChanged(value: Int) {
        updateState(productDownloadDetailsViewState.copy(downloadExpiry = value))
    }

    fun onDownloadLimitChanged(value: Int) {
        updateState(productDownloadDetailsViewState.copy(downloadLimit = value))
    }

    fun onDoneOrUpdateClicked() {
        // TODO handle file creation by checking if the navArgs file is null
        triggerEvent(
            UpdateFileAndExitEvent(
                productDownloadDetailsViewState.fileDraft,
                productDownloadDetailsViewState.downloadLimit,
                productDownloadDetailsViewState.downloadExpiry
            )
        )
    }

    fun onBackButtonClicked(): Boolean {
        return if (hasChanges) {
            triggerEvent(ShowDiscardDialog(
                positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                    triggerEvent(Exit)
                }
            ))
            false
        } else true
    }

    private fun updateState(updatedState: ProductDownloadDetailsViewState) {
        val hasChanges = updatedState.fileDraft != navArgs.productFile ||
            updatedState.downloadExpiry != navArgs.downloadExpiry ||
            updatedState.downloadLimit != navArgs.downloadLimit
        productDownloadDetailsViewState = updatedState.copy(hasChanges = hasChanges)
    }

    sealed class ProductDownloadDetailsEvent : Event() {
        data class UpdateFileAndExitEvent(
            val updatedFile: ProductFile,
            val downloadLimit: Int,
            val downloadExpiry: Int
        ) : ProductDownloadDetailsEvent()
    }

    @Parcelize
    data class ProductDownloadDetailsViewState(
        val fileDraft: ProductFile,
        val downloadLimit: Int,
        val downloadExpiry: Int,
        val hasChanges: Boolean
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductDownloadDetailsViewModel>
}
