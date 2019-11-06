package com.woocommerce.android.ui.products

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.media.MediaRemovalService
import com.woocommerce.android.media.MediaRemovalService.Companion.OnProductImageRemovalCompletedEvent
import com.woocommerce.android.media.MediaRemovalService.Companion.OnProductImageRemovalStartedEvent
import com.woocommerce.android.media.MediaRemovalWrapper
import com.woocommerce.android.media.MediaUploadService
import com.woocommerce.android.media.MediaUploadService.Companion.OnProductImagesUploadCompletedEvent
import com.woocommerce.android.media.MediaUploadService.Companion.OnProductImagesUploadStartedEvent
import com.woocommerce.android.media.MediaUploadWrapper
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
    private val mediaUploadWrapper: MediaUploadWrapper,
    private val mediaRemovalWrapper: MediaRemovalWrapper
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

    private val _removingProductRemoteMediaId = MutableLiveData<Long>()
    val removingProductRemoteMediaId: LiveData<Long> = _removingProductRemoteMediaId

    private val _exit = SingleLiveEvent<Unit>()
    val exit: LiveData<Unit> = _exit

    init {
        EventBus.getDefault().register(this)
    }

    fun start(remoteProductId: Long) {
        this.remoteProductId = remoteProductId
        loadProduct()
        _isUploadingProductImage.value = MediaUploadService.isUploadingForProduct(remoteProductId)
    }

    override fun onCleared() {
        super.onCleared()
        productRepository.onCleanup()
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
        if (MediaUploadService.isBusy()) {
            _showSnackbarMessage.value = R.string.product_image_service_busy
            return
        }
        _isUploadingProductImage.value = true
        mediaUploadWrapper.uploadProductMedia(remoteProductId, localImageUri)
    }

    fun removeProductMedia(remoteProductId: Long, remoteMediaId: Long) {
        if (MediaRemovalService.isBusy()) {
            _showSnackbarMessage.value = R.string.product_image_service_busy
            return
        }
        mediaRemovalWrapper.removeProductMedia(remoteProductId, remoteMediaId)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnProductImagesUploadStartedEvent) {
        if (remoteProductId == event.remoteProductId) {
            _isUploadingProductImage.value = true
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnProductImagesUploadCompletedEvent) {
        _isUploadingProductImage.value = false
        if (event.isError) {
            _showSnackbarMessage.value = R.string.product_image_service_error
        } else {
            loadProduct()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductImageRemovalStartedEvent(event: OnProductImageRemovalStartedEvent) {
        if (remoteProductId == event.remoteProductId) {
            _removingProductRemoteMediaId.value = event.remoteMediaId
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductImageRemovalCompletedEvent(event: OnProductImageRemovalCompletedEvent) {
        _removingProductRemoteMediaId.value = 0
        if (event.isError) {
            _showSnackbarMessage.value = R.string.product_image_service_error
        } else {
            loadProduct()
        }
    }
}
