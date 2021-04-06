package com.woocommerce.android.ui.products.downloads

import android.net.Uri
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
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

    @AssistedFactory
    interface Factory : ViewModelAssistedFactory<AddProductDownloadViewModel>
}
