package com.woocommerce.android.ui.imageviewer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImagesUpdateCompletedEvent
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
class ImageViewerViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val repository: ImageViewerRepository,
    private val networkStatus: NetworkStatus
) : ScopedViewModel(mainDispatcher) {
    private var remoteProductId = 0L

    private val _product = MutableLiveData<Product>()
    val product: LiveData<Product> = _product

    private val _showSnackbarMessage = SingleLiveEvent<Int>()
    val showSnackbarMessage: LiveData<Int> = _showSnackbarMessage

    private val _exit = SingleLiveEvent<Unit>()
    val exit: LiveData<Unit> = _exit

    init {
        EventBus.getDefault().register(this)
    }

    fun start(remoteProductId: Long) {
        this.remoteProductId = remoteProductId
        loadProduct()
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    fun loadProduct() {
        _product.value = repository.getProduct(remoteProductId)
    }

    fun removeProductImage(remoteMediaId: Long) {
        if (!checkNetwork()) {
            return
        }

        if (repository.removeProductImage(remoteProductId, remoteMediaId)) {
            AnalyticsTracker.track(Stat.PRODUCT_IMAGE_REMOVED)
            // reload the product to reflect the removed image
            loadProduct()
        } else {
            _showSnackbarMessage.value = R.string.product_image_error_removing
        }
    }

    private fun checkNetwork(): Boolean {
        if (networkStatus.isConnected()) {
            return true
        }
        _showSnackbarMessage.value = R.string.network_activity_no_connectivity
        return false
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnProductImagesUpdateCompletedEvent) {
        if (remoteProductId == event.remoteProductId) {
            if (event.isError) {
                _showSnackbarMessage.value = R.string.product_image_error_removing
            } else {
                loadProduct()
            }
        }
    }
}
