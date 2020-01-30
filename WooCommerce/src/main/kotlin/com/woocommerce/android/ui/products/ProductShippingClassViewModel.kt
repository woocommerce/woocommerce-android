package com.woocommerce.android.ui.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.WCProductShippingClassModel

class ProductShippingClassViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val productShippingClassRepository: ProductShippingClassRepository
) : ScopedViewModel(savedState, dispatchers) {
    private var shippingClassLoadJob: Job? = null

    private val _productShippingClasses = MutableLiveData<List<WCProductShippingClassModel>>()
    val productShippingClasses: LiveData<List<WCProductShippingClassModel>> = _productShippingClasses

    init {
        loadProductShippingClasses()
    }

    override fun onCleared() {
        super.onCleared()
        productShippingClassRepository.onCleanup()
    }

    fun loadProductShippingClasses(loadMore: Boolean = false) {
        waitForExistingShippingClassLoad()

        shippingClassLoadJob = launch {
            // first get the shipping classes from the db
            if (!loadMore) {
                val shippingClasses = productShippingClassRepository.getProductShippingClasses()
                _productShippingClasses.value = shippingClasses
            }

            // then fetch an updated list
            _productShippingClasses.value = productShippingClassRepository.fetchProductShippingClasses(loadMore)
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

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductShippingClassViewModel>
}
