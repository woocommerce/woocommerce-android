package com.woocommerce.android.ui.products.downloads

import android.content.DialogInterface
import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ProductFile
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
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
        ProductDownloadDetailsViewState(navArgs.productFile ?: ProductFile(null, "", ""), false)
    )
    private var productDownloadDetailsViewState by productDownloadDetailsViewStateData

    val hasChanges
        get() = productDownloadDetailsViewState.hasChanges

    val screenTitle
        get() = navArgs.productFile?.name ?: TODO("Should be implemented for files creation")

    fun onFileUrlChanged(url: String) {
        val updatedDraft = productDownloadDetailsViewState.fileDraft.copy(url = url)
        updateDraftFile(updatedDraft)
    }

    fun onFileNameChanged(name: String) {
        val updatedDraft = productDownloadDetailsViewState.fileDraft.copy(name = name)
        updateDraftFile(updatedDraft)
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

    private fun updateDraftFile(updatedFile: ProductFile){
        productDownloadDetailsViewState = productDownloadDetailsViewState.copy(
            fileDraft = updatedFile,
            hasChanges = updatedFile != navArgs.productFile
        )
    }

    @Parcelize
    data class ProductDownloadDetailsViewState(
        val fileDraft: ProductFile,
        val hasChanges: Boolean
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductDownloadDetailsViewModel>
}
