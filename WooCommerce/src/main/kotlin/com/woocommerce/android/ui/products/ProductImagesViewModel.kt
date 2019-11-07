package com.woocommerce.android.ui.products

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.media.ProductImagesService
import com.woocommerce.android.media.ProductImagesService.Companion.Action
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImagesUpdateCompletedEvent
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImagesUpdateStartedEvent
import com.woocommerce.android.media.ProductImagesServiceWrapper
import com.woocommerce.android.model.Product
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
    private val productImagesServiceWrapper: ProductImagesServiceWrapper
) : ScopedViewModel(mainDispatcher) {
    private var remoteProductId = 0L
    var removingRemoteMediaId = 0L

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

    private val _isRemovingProductImage = MutableLiveData<Boolean>()
    val isRemovingProductImage: LiveData<Boolean> = _isRemovingProductImage

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
        if (ProductImagesService.isBusy()) {
            _showSnackbarMessage.value = R.string.product_image_service_busy
            return
        }
        productImagesServiceWrapper.uploadProductMedia(remoteProductId, localImageUri)
    }

    fun removeProductMedia(remoteProductId: Long, remoteMediaId: Long) {
        if (ProductImagesService.isBusy()) {
            _showSnackbarMessage.value = R.string.product_image_service_busy
            return
        }
        removingRemoteMediaId = remoteMediaId
        productImagesServiceWrapper.removeProductMedia(remoteProductId, remoteMediaId)
    }

    private fun setIsUploadingImage(isUploading: Boolean) {
        if (isUploading != _isUploadingProductImage.value) {
            _isUploadingProductImage.value = isUploading
        }
    }

    private fun setIsRemovingImage(isRemoving: Boolean) {
        if (isRemoving != _isRemovingProductImage.value) {
            _isRemovingProductImage.value = isRemoving
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnProductImagesUpdateStartedEvent) {
        if (remoteProductId == event.remoteProductId) {
            if (event.action == Action.UPLOAD_IMAGE) {
                setIsUploadingImage(true)
            } else if (event.action == Action.REMOVE_IMAGE) {
                setIsRemovingImage(true)
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnProductImagesUpdateCompletedEvent) {
        if (event.action == Action.UPLOAD_IMAGE) {
            setIsUploadingImage(false)
        } else if (event.action == Action.REMOVE_IMAGE) {
            setIsRemovingImage(false)
            removingRemoteMediaId = 0
        }

        if (event.isError) {
            _showSnackbarMessage.value = R.string.product_image_service_error
        } else {
            loadProduct()
        }
    }
}
