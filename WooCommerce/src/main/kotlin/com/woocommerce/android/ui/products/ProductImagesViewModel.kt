package com.woocommerce.android.ui.products

import android.net.Uri
import android.os.Parcelable
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R.string
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_DETAIL_IMAGE_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_IMAGE_SETTINGS_ADD_IMAGES_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.areSameImagesAs
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.media.MediaFileUploadHandler
import com.woocommerce.android.ui.media.getMediaUploadErrorMessage
import com.woocommerce.android.ui.products.ProductImagesViewModel.ProductImagesState.Browsing
import com.woocommerce.android.ui.products.ProductImagesViewModel.ProductImagesState.Dragging
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewMediaUploadErrors
import com.woocommerce.android.util.swap
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.*
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ProductImagesViewModel @Inject constructor(
    private val networkStatus: NetworkStatus,
    private val mediaFileUploadHandler: MediaFileUploadHandler,
    private val resourceProvider: ResourceProvider,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val navArgs: ProductImagesFragmentArgs by savedState.navArgs()
    private val originalImages = navArgs.images.toList()

    val isMultiSelectionAllowed = navArgs.requestCode == RequestCodes.PRODUCT_DETAIL_IMAGES

    val viewStateData = LiveDataDelegate(
        savedState,
        ViewState(
            showSourceChooser = navArgs.showChooser,
            uploadingImageUris = emptyList(),
            isImageDeletingAllowed = true,
            images = navArgs.images.toList(),
            isWarningVisible = !isMultiSelectionAllowed,
            isDragDropDescriptionVisible = isMultiSelectionAllowed
        )
    ) { old, new ->
        if (old != new) {
            updateButtonStates()
            updateDragAndDropDescriptionStates()
        }
    }
    private var viewState by viewStateData

    val images
        get() = viewState.images ?: emptyList()

    val isImageDeletingAllowed
        get() = viewState.isImageDeletingAllowed ?: true

    init {
        if (viewState.showSourceChooser == true) {
            viewState = viewState.copy(showSourceChooser = false)
            clearImageUploadErrors()
            triggerEvent(ShowImageSourceDialog)
        } else if (navArgs.selectedImage != null) {
            triggerEvent(ShowImageDetail(navArgs.selectedImage!!, true))
        }

        observeImageUploadEvents()
    }

    fun uploadProductImages(remoteProductId: Long, localUriList: List<Uri>) {
        if (!networkStatus.isConnected()) {
            triggerEvent(ShowSnackbar(string.network_activity_no_connectivity))
            return
        }

        mediaFileUploadHandler.enqueueUpload(remoteProductId, localUriList.map { it.toString() })

        if (!isMultiSelectionAllowed) {
            viewState = viewState.copy(images = emptyList())
        }
    }

    fun onShowStorageChooserButtonClicked() {
        AnalyticsTracker.track(
            AnalyticsEvent.PRODUCT_IMAGE_SETTINGS_ADD_IMAGES_SOURCE_TAPPED,
            mapOf(AnalyticsTracker.KEY_IMAGE_SOURCE to AnalyticsTracker.IMAGE_SOURCE_DEVICE)
        )
        triggerEvent(ShowStorageChooser)
    }

    fun onShowCameraButtonClicked() {
        AnalyticsTracker.track(
            AnalyticsEvent.PRODUCT_IMAGE_SETTINGS_ADD_IMAGES_SOURCE_TAPPED,
            mapOf(AnalyticsTracker.KEY_IMAGE_SOURCE to AnalyticsTracker.IMAGE_SOURCE_CAMERA)
        )
        triggerEvent(ShowCamera)
    }

    fun onShowWPMediaPickerButtonClicked() {
        AnalyticsTracker.track(
            AnalyticsEvent.PRODUCT_IMAGE_SETTINGS_ADD_IMAGES_SOURCE_TAPPED,
            mapOf(AnalyticsTracker.KEY_IMAGE_SOURCE to AnalyticsTracker.IMAGE_SOURCE_WPMEDIA)
        )
        triggerEvent(ShowWPMediaPicker)
    }

    fun onImageRemoved(imageId: Long) {
        viewState = viewState.copy(images = images.filter { it.id != imageId })
    }

    fun onMediaLibraryImagesAdded(newImages: List<Image>) {
        viewState = if (isMultiSelectionAllowed) {
            viewState.copy(images = images + newImages)
        } else {
            viewState.copy(images = newImages)
        }
    }

    fun onImageSourceButtonClicked() {
        clearImageUploadErrors()
        AnalyticsTracker.track(PRODUCT_IMAGE_SETTINGS_ADD_IMAGES_BUTTON_TAPPED)
        triggerEvent(ShowImageSourceDialog)
    }

    fun onGalleryImageClicked(image: Image) {
        AnalyticsTracker.track(PRODUCT_DETAIL_IMAGE_TAPPED)
        triggerEvent(ShowImageDetail(image))
    }

    fun onValidateButtonClicked() {
        viewState = viewState.copy(productImagesState = Browsing)
    }

    fun onNavigateBackButtonClicked() {
        when (val productImagesState = viewState.productImagesState) {
            is Dragging -> {
                viewState = viewState.copy(
                    productImagesState = Browsing,
                    images = productImagesState.initialState
                )
            }
            Browsing -> {
                if (images.areSameImagesAs(originalImages)) {
                    triggerEvent(Exit)
                } else {
                    triggerEvent(ExitWithResult(images))
                }
            }
        }
    }

    private fun updateButtonStates() {
        val numImages = (viewState.images?.size ?: 0) + (viewState.uploadingImageUris?.size ?: 0)
        viewState = viewState.copy(
            chooserButtonButtonTitleRes = when {
                isMultiSelectionAllowed -> string.product_add_photos
                numImages > 0 -> string.product_replace_photo
                else -> string.product_add_photo
            }
        )
    }

    private fun updateDragAndDropDescriptionStates() {
        viewState = viewState.copy(
            isDragDropDescriptionVisible = viewState.productImagesState is Dragging || images.size > 1
        )
    }

    private fun clearImageUploadErrors() {
        // clear existing image upload errors from the backlog
        mediaFileUploadHandler.clearImageErrors(navArgs.remoteId)
    }

    private fun observeImageUploadEvents() {
        val remoteProductId = navArgs.remoteId
        mediaFileUploadHandler.observeCurrentUploads(remoteProductId)
            .map { list -> list.map { it.toUri() } }
            .onEach { viewState = viewState.copy(uploadingImageUris = it) }
            .launchIn(this)

        mediaFileUploadHandler.observeSuccessfulUploads(remoteProductId)
            .onEach { media ->
                viewState = if (isMultiSelectionAllowed) {
                    viewState.copy(images = images + media.toAppModel())
                } else {
                    viewState.copy(images = listOf(media.toAppModel()))
                }
            }
            .launchIn(this)

        mediaFileUploadHandler.observeCurrentUploadErrors(remoteProductId)
            .filter { it.isNotEmpty() }
            .onEach {
                val errorMsg = resourceProvider.getMediaUploadErrorMessage(it.size)
                triggerEvent(
                    ShowActionSnackbar(errorMsg) { triggerEvent(ViewMediaUploadErrors(remoteProductId)) }
                )
            }
            .launchIn(this)
    }

    fun onGalleryImageDragStarted() {
        when (viewState.productImagesState) {
            is Dragging -> { /* no-op*/ }
            Browsing -> viewState = viewState.copy(productImagesState = Dragging(images))
        }
    }

    fun onGalleryImageDeleteIconClicked(image: Image) {
        triggerEvent(ShowDeleteImageConfirmation(image))
    }

    fun onDeleteImageConfirmed(image: Image) {
        viewState = viewState.copy(images = images - image)
    }

    fun onGalleryImageMoved(from: Int, to: Int) {
        val canSwap = from >= 0 && from < images.size && to >= 0 && to < images.size
        if (canSwap) {
            val reorderedImages = images.swap(from, to)
            viewState = viewState.copy(images = reorderedImages)
        }
    }

    @Parcelize
    data class ViewState(
        val showSourceChooser: Boolean? = null,
        val uploadingImageUris: List<Uri>? = null,
        val isImageDeletingAllowed: Boolean? = null,
        val images: List<Image>? = null,
        val chooserButtonButtonTitleRes: Int? = null,
        val isWarningVisible: Boolean? = null,
        val isDragDropDescriptionVisible: Boolean? = null,
        val productImagesState: ProductImagesState = Browsing
    ) : Parcelable

    object ShowImageSourceDialog : Event()
    object ShowStorageChooser : Event()
    object ShowCamera : Event()
    object ShowWPMediaPicker : Event()
    data class ShowDeleteImageConfirmation(val image: Image) : Event()
    data class ShowImageDetail(val image: Image, val isOpenedDirectly: Boolean = false) : Event()

    sealed class ProductImagesState : Parcelable {
        @Parcelize
        data class Dragging(val initialState: List<Image>) : ProductImagesState()
        @Parcelize
        object Browsing : ProductImagesState()
    }
}
