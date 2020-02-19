package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.WCProductShippingClassModel

class ProductShippingClassViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val productShippingClassRepository: ProductShippingClassRepository
) : ScopedViewModel(savedState, dispatchers) {
    private val navArgs: ProductShippingClassFragmentArgs by savedState.navArgs()
    private var shippingClassLoadJob: Job? = null

    private val _productShippingClasses = MutableLiveData<List<WCProductShippingClassModel>>()
    val productShippingClasses: LiveData<List<WCProductShippingClassModel>> = _productShippingClasses

    val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateLiveData

    val selectedShippingClassSlug: String
        get() = navArgs.shippingClassSlug

    init {
        loadProductShippingClasses()
    }

    override fun onCleared() {
        super.onCleared()
        productShippingClassRepository.onCleanup()
    }

    fun loadProductShippingClasses(loadMore: Boolean = false) {
        if (loadMore && !productShippingClassRepository.canLoadMoreShippingClasses) {
            WooLog.d(T.PRODUCTS, "Can't load more product shipping classes")
            return
        }

        waitForExistingShippingClassLoad()

        shippingClassLoadJob = launch {
            if (loadMore) {
                viewState = viewState.copy(showLoadingMoreProgress = true)
            } else {
                // first get the shipping classes from the db before fetching from backend
                val shippingClasses = productShippingClassRepository.getProductShippingClasses()
                _productShippingClasses.value = shippingClasses
                if (shippingClasses.isEmpty()) {
                    viewState = viewState.copy(showLoadingProgress = true)
                }
            }

            _productShippingClasses.value = productShippingClassRepository.fetchProductShippingClasses(loadMore)
            viewState = viewState.copy(showLoadingProgress = false, showLoadingMoreProgress = false)
        }
    }

    private fun waitForExistingShippingClassLoad() {
        if (shippingClassLoadJob?.isActive == true) {
            launch {
                try {
                    shippingClassLoadJob?.join()
                } catch (e: CancellationException) {
                    WooLog.d(
                            T.PRODUCTS,
                            "CancellationException while waiting for existing shipping class list fetch"
                    )
                }
            }
        }
    }

    @Parcelize
    data class ViewState(
        val showLoadingProgress: Boolean = false,
        val showLoadingMoreProgress: Boolean = false
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductShippingClassViewModel>
}
