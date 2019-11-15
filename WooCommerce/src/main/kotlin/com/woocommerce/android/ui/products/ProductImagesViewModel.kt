package com.woocommerce.android.ui.products

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.media.ProductImagesService
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImagesUpdateCompletedEvent
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImagesUpdateStartedEvent
import com.woocommerce.android.media.ProductImagesServiceWrapper
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import kotlinx.coroutines.CoroutineDispatcher
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject
import javax.inject.Named

@OpenClassOnDebug
class ProductImagesViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val productRepository: ProductImagesRepository,
    private val productImagesServiceWrapper: ProductImagesServiceWrapper,
    private val networkStatus: NetworkStatus
) : ScopedViewModel(mainDispatcher) {
    private var remoteProductId = 0L

    private val _product = MutableLiveData<Product>()
    val product: LiveData<Product> = _product

    private val _showSnackbarMessage = SingleLiveEvent<Int>()
    val showSnackbarMessage: LiveData<Int> = _showSnackbarMessage

    private val _chooseProductImage = SingleLiveEvent<Unit>()
    val chooseProductImage: LiveData<Unit> = _chooseProductImage

    private val _captureProductImage = SingleLiveEvent<Unit>()
    val captureProductImage: LiveData<Unit> = _captureProductImage

    private val _isUploadingProductImage = MutableLiveData<Boolean>()
    val isUploadingProductImage: LiveData<Boolean> = _isUploadingProductImage

    private val _exit = SingleLiveEvent<Unit>()
    val exit: LiveData<Unit> = _exit

    init {
        EventBus.getDefault().register(this)
    }

    fun start(remoteProductId: Long) {
        this.remoteProductId = remoteProductId
        loadProduct()

        val isUploading = ProductImagesService.isUploadingForProduct(remoteProductId)
        setIsUploadingImage(isUploading)
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

    fun uploadProductMedia(remoteProductId: Long, localImageUri: Uri) {
        if (!checkNetwork()) {
            return
        }
        if (ProductImagesService.isBusy()) {
            _showSnackbarMessage.value = R.string.product_image_service_busy
            return
        }
        productImagesServiceWrapper.uploadProductMedia(remoteProductId, localImageUri)
    }

    fun removeProductMedia(remoteProductId: Long, remoteMediaId: Long) {
        if (!checkNetwork()) {
            return
        }
        if (productRepository.removeProductImage(remoteProductId, remoteMediaId)) {
            // reload the product to reflect the removed image
            loadProduct()
        } else {
            _showSnackbarMessage.value = R.string.product_image_service_error_removing
        }
    }

    private fun checkNetwork(): Boolean {
        if (networkStatus.isConnected()) {
            return true
        }
        _showSnackbarMessage.value = R.string.network_activity_no_connectivity
        return false
    }

    private fun setIsUploadingImage(isUploading: Boolean) {
        if (isUploading != _isUploadingProductImage.value) {
            _isUploadingProductImage.value = isUploading
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnProductImagesUpdateStartedEvent) {
        if (remoteProductId == event.remoteProductId) {
            setIsUploadingImage(true)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnProductImagesUpdateCompletedEvent) {
        if (remoteProductId == event.remoteProductId) {
            setIsUploadingImage(false)
            if (event.isError) {
                _showSnackbarMessage.value = R.string.product_image_service_error_uploading
            } else {
                loadProduct()
            }
        }
    }
}
