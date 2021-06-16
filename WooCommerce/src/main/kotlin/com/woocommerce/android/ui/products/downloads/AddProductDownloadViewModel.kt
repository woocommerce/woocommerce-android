package com.woocommerce.android.ui.products.downloads

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.ProductFile
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDownloadDetails
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddProductDownloadViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    fun onMediaGalleryClicked() {
        triggerEvent(PickFileFromMedialLibrary)
    }

    fun onDeviceClicked() {
        triggerEvent(PickFileFromDevice)
    }

    fun onEnterURLClicked() {
        triggerEvent(
            ViewProductDownloadDetails(
                isEditing = false,
                file = ProductFile(id = null, url = "", name = "")
            )
        )
    }

    fun launchFileUpload(uri: Uri) {
        triggerEvent(UploadFile(uri))
    }

    object PickFileFromMedialLibrary : Event()
    object PickFileFromDevice : Event()
    data class UploadFile(val uri: Uri) : Event()
}
