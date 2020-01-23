package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.WCProductShippingClassModel
import org.wordpress.android.fluxc.store.WooCommerceStore

class ProductShippingViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    selectedSite: SelectedSite,
    wooCommerceStore: WooCommerceStore,
    private val productRepository: ProductDetailRepository,
    private val productShippingRepository: ProductShippingRepository
) : ScopedViewModel(savedState, dispatchers) {
    val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateLiveData
    private var shippingClassLoadJob: Job? = null

    private val _productShippingClasses = MutableLiveData<List<WCProductShippingClassModel>>()
    val productShippingClasses: LiveData<List<WCProductShippingClassModel>> = _productShippingClasses

    var weightUnit: String? = null
        private set
    var dimensionUnit: String? = null
        private set

    init {
        val settings = wooCommerceStore.getProductSettings(selectedSite.get())
        weightUnit = settings?.weightUnit
        dimensionUnit = settings?.dimensionUnit
    }

    fun start(remoteProductId: Long) {
        loadProduct(remoteProductId)
    }

    override fun onCleared() {
        super.onCleared()
        productShippingRepository.onCleanup()
    }

    private fun loadProduct(remoteProductId: Long) {
        launch {
            val productInDb = productRepository.getProduct(remoteProductId)
            if (productInDb != null) {
                viewState = viewState.copy(product = productInDb)
            }
        }
    }

    fun loadProductShippingClasses(loadMore: Boolean = false) {
        waitForExistingShippingClassLoad()

        shippingClassLoadJob = launch {
            // first get the shipping classes from the db
            if (!loadMore) {
                val shippingClasses = productShippingRepository.getProductShippingClasses()
                _productShippingClasses.value = shippingClasses
            }

            // then fetch an updated list
            _productShippingClasses.value = productShippingRepository.fetchProductShippingClasses(loadMore)
        }
    }

    private fun waitForExistingShippingClassLoad() {
        if (shippingClassLoadJob?.isActive == true) {
            launch {
                try {
                    shippingClassLoadJob?.join()
                } catch (e: CancellationException) {
                    WooLog.d(
                            WooLog.T.PRODUCTS,
                            "CancellationException while waiting for existing shipping class list fetch"
                    )
                }
            }
        }
    }

    @Parcelize
    data class ViewState(
        val product: Product? = null,
        val isProductUpdated: Boolean? = null,
        val shouldShowDiscardDialog: Boolean = true
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductShippingViewModel>
}
