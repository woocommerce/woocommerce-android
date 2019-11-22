package com.woocommerce.android.ui.products

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.media.ProductImagesService
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImageUploaded
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImagesUpdateCompletedEvent
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImagesUpdateStartedEvent
import com.woocommerce.android.media.ProductImagesServiceWrapper
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

@OpenClassOnDebug
class ProductImagesViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val productRepository: ProductImagesRepository,
    private val productImagesServiceWrapper: ProductImagesServiceWrapper,
    private val networkStatus: NetworkStatus
) : ScopedViewModel(savedState, dispatchers) {
    private var remoteProductId = 0L

    private val _product = MutableLiveData<Product>()
    val product: LiveData<Product> = _product

    private val _showSnackbarMessage = SingleLiveEvent<Int>()
    val showSnackbarMessage: LiveData<Int> = _showSnackbarMessage

    private val _chooseProductImage = SingleLiveEvent<Unit>()
    val chooseProductImage: LiveData<Unit> = _chooseProductImage

    private val _captureProductImage = SingleLiveEvent<Unit>()
    val captureProductImage: LiveData<Unit> = _captureProductImage

    private val _uploadingImageUris = MutableLiveData<List<Uri>>()
    val uploadingImageUris: LiveData<List<Uri>> = _uploadingImageUris

    private val _exit = SingleLiveEvent<Unit>()
    val exit: LiveData<Unit> = _exit

    init {
        EventBus.getDefault().register(this)
    }

    fun start(remoteProductId: Long) {
        this.remoteProductId = remoteProductId
        loadProduct()
        checkUploads()
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    fun loadProduct() {
        _product.value = productRepository.getProduct(remoteProductId)
    }

    fun onChooseImageClicked() {
        _chooseProductImage.call()
    }

    fun onCaptureImageClicked() {
        _captureProductImage.call()
    }

    fun uploadProductImages(remoteProductId: Long, localUriList: ArrayList<Uri>) {
        if (!checkNetwork()) {
            return
        }
        if (ProductImagesService.isBusy()) {
            _showSnackbarMessage.value = R.string.product_image_service_busy
            return
        }
        productImagesServiceWrapper.uploadProductMedia(remoteProductId, localUriList)
    }

    private fun checkNetwork(): Boolean {
        if (networkStatus.isConnected()) {
            return true
        }
        _showSnackbarMessage.value = R.string.network_activity_no_connectivity
        return false
    }

    private fun checkUploads() {
        _uploadingImageUris.value = ProductImagesService.getUploadingImageUrisForProduct(remoteProductId)
    }

    /**
     * The list of images has started uploaded
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnProductImagesUpdateStartedEvent) {
        if (remoteProductId == event.remoteProductId) {
            checkUploads()
        }
    }

    /**
     * The list of images has finished uploading
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnProductImagesUpdateCompletedEvent) {
        if (remoteProductId == event.remoteProductId) {
            loadProduct()
            checkUploads()
        }
    }

    /**
     * A single image has finished uploading
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnProductImageUploaded) {
        if (remoteProductId == event.remoteProductId) {
            if (event.isError) {
                _showSnackbarMessage.value = R.string.product_image_service_error_uploading
            } else {
                loadProduct()
            }
            checkUploads()
        }
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductImagesViewModel>
}
