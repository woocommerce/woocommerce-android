package com.woocommerce.android.ui.products.downloads

import android.net.Uri
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ProductFile
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDownloadDetails
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel

class AddProductDownloadViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers
) : ScopedViewModel(savedState, dispatchers) {
    fun onMediaGalleryClicked() {
        triggerEvent(PickFileFromMedialLibrary)
    }

    fun onDeviceClicked() {
        triggerEvent(PickFileFromDevice)
    }

    fun onCameraClicked() {
        triggerEvent(PickFileFromCamera)
    }

    fun navigateToFileDetails(url: String) {
        triggerEvent(
            ViewProductDownloadDetails(
                isEditing = false,
                file = ProductFile(id = null, url = url, name = "")
            )
        )
    }

    fun launchFileUpload(uri: Uri) {
        triggerEvent(UploadFile(uri))
    }

    object PickFileFromMedialLibrary : Event()
    object PickFileFromDevice : Event()
    object PickFileFromCamera : Event()
    data class UploadFile(val uri: Uri) : Event()

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<AddProductDownloadViewModel>
}
