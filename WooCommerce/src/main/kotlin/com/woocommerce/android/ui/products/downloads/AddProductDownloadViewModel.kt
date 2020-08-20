package com.woocommerce.android.ui.products.downloads

import android.net.Uri
import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ProductFile
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDownloadDetails
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize

class AddProductDownloadViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers
) : ScopedViewModel(savedState, dispatchers) {
    val viewStateData = LiveDataDelegate(
        savedState,
        AddProductDownloadViewState(isUploading = false)
    )
    private var viewState by viewStateData

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

    @Parcelize
    data class AddProductDownloadViewState(
        val isUploading: Boolean
    ) : Parcelable

    object PickFileFromMedialLibrary : Event()
    object PickFileFromDevice : Event()
    object PickFileFromCamera : Event()
    data class UploadFile(val uri: Uri): Event()

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<AddProductDownloadViewModel>
}
