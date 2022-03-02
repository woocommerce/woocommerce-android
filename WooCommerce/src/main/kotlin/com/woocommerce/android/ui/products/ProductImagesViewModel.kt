package com.woocommerce.android.ui.products

import android.net.Uri
import android.os.Parcelable
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R.string
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_IMAGE_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_IMAGE_SETTINGS_ADD_IMAGES_BUTTON_TAPPED
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
    private var originalImages = navArgs.images.toList()

    val isMultiSelectionAllowed = navArgs.requestCode == RequestCodes.PRODUCT_DETAIL_IMAGES

    private val _productImages = MutableLiveData<List<Image>>()
    val productImages: LiveData<List<Image>> = _productImages

    private val _uploadingImageUris = MutableLiveData<List<Uri>>()
    val uploadingImageUris: LiveData<List<Uri>> = _uploadingImageUris

    private val _productImagesState = MutableLiveData<ProductImagesState>()
    val productImagesState: LiveData<ProductImagesState> = _productImagesState

    val viewStateData = LiveDataDelegate(
        savedState,
        ViewState(
            showSourceChooser = navArgs.showChooser,
            isImageDeletingAllowed = true,
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

    private val imageCount
        get() = _productImages.value?.size ?: 0

    private val uploadingImageCount
        get() = _uploadingImageUris.value?.size ?: 0

    val isImageDeletingAllowed
        get() = viewState.isImageDeletingAllowed ?: true

    init {
        if (viewState.isInitialCreation) {
            viewState = viewState.copy(isInitialCreation = false)
            _productImages.value = originalImages
            _productImagesState.value = Browsing
        }

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
            _productImages.value = emptyList()
        }
    }

    fun onShowStorageChooserButtonClicked() {
        AnalyticsTracker.track(
            Stat.PRODUCT_IMAGE_SETTINGS_ADD_IMAGES_SOURCE_TAPPED,
            mapOf(AnalyticsTracker.KEY_IMAGE_SOURCE to AnalyticsTracker.IMAGE_SOURCE_DEVICE)
        )
        triggerEvent(ShowStorageChooser)
    }

    fun onShowCameraButtonClicked() {
        AnalyticsTracker.track(
            Stat.PRODUCT_IMAGE_SETTINGS_ADD_IMAGES_SOURCE_TAPPED,
            mapOf(AnalyticsTracker.KEY_IMAGE_SOURCE to AnalyticsTracker.IMAGE_SOURCE_CAMERA)
        )
        triggerEvent(ShowCamera)
    }

    fun onShowWPMediaPickerButtonClicked() {
        AnalyticsTracker.track(
            Stat.PRODUCT_IMAGE_SETTINGS_ADD_IMAGES_SOURCE_TAPPED,
            mapOf(AnalyticsTracker.KEY_IMAGE_SOURCE to AnalyticsTracker.IMAGE_SOURCE_WPMEDIA)
        )
        triggerEvent(ShowWPMediaPicker)
    }

    fun onImageRemoved(imageId: Long) {
        _productImages.value = _productImages.value?.filter { it.id != imageId }
    }

    fun onMediaLibraryImagesAdded(newImages: List<Image>) {
        addImages(newImages)
    }

    private fun addImages(images: List<Image>) {
        if (isMultiSelectionAllowed) {
            _productImages.value = _productImages.value?.let {
                it + images
            } ?: images
        } else {
            _productImages.value = images
        }
    }

    private fun addSingleImage(image: Image) {
        addImages(listOf(image))
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
        _productImagesState.value = Browsing
    }

    fun onNavigateBackButtonClicked() {
        when (val productImagesState = _productImagesState.value) {
            is Dragging -> {
                _productImagesState.value = Browsing
                _productImages.value = productImagesState.initialState
            }
            Browsing -> {
                if (_productImages.value?.areSameImagesAs(originalImages) == true) {
                    triggerEvent(Exit)
                } else {
                    triggerEvent(ExitWithResult(_productImages.value))
                }
            }
        }
    }

    private fun updateButtonStates() {
        val numImages = imageCount + uploadingImageCount
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
            isDragDropDescriptionVisible = _productImagesState.value is Dragging || imageCount > 1
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
            .onEach { _uploadingImageUris.value = it }
            .launchIn(this)

        mediaFileUploadHandler.observeSuccessfulUploads(remoteProductId)
            .onEach { media ->
                addSingleImage(media.toAppModel())
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
        if (_productImagesState.value == Browsing) {
            _productImagesState.value = Dragging(_productImages.value!!)
        }
    }

    fun onGalleryImageDeleteIconClicked(image: Image) {
        triggerEvent(ShowDeleteImageConfirmation(image))
    }

    fun onDeleteImageConfirmed(image: Image) {
        _productImages.value = _productImages.value!! - image
    }

    fun onGalleryImageMoved(from: Int, to: Int) {
        val canSwap = from >= 0 && from < imageCount && to >= 0 && to < imageCount
        if (canSwap) {
            _productImages.value = _productImages.value!!.swap(from, to)
        }
    }

    @Parcelize
    data class ViewState(
        val showSourceChooser: Boolean? = null,
        val isImageDeletingAllowed: Boolean? = null,
        val chooserButtonButtonTitleRes: Int? = null,
        val isWarningVisible: Boolean? = null,
        val isDragDropDescriptionVisible: Boolean? = null,
        val isInitialCreation: Boolean = true
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
