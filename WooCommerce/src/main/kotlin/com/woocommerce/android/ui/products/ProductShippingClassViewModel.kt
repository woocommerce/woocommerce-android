package com.woocommerce.android.ui.products

import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ShippingClass
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ProductShippingClassViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val productRepository: ProductShippingClassRepository
) : ScopedViewModel(savedState, dispatchers) {
    private var shippingClassLoadJob: Job? = null

    // view state for the shipping class screen
    final val productShippingClassViewStateData = LiveDataDelegate(savedState, ProductShippingClassViewState())
    private var productShippingClassViewState by productShippingClassViewStateData

    /**
     * Load & fetch the shipping classes for the current site, optionally performing a "load more" to
     * load the next page of shipping classes
     */
    fun loadShippingClasses(loadMore: Boolean = false) {
        if (loadMore && !productRepository.canLoadMoreShippingClasses) {
            WooLog.d(T.PRODUCTS, "Can't load more product shipping classes")
            return
        }

        waitForExistingShippingClassFetch()

        shippingClassLoadJob = launch {
            productShippingClassViewState = if (loadMore) {
                productShippingClassViewState.copy(isLoadingMoreProgressShown = true)
            } else {
                // get cached shipping classes and only show loading progress the list is empty, otherwise show
                // them right away
                val cachedShippingClasses = productRepository.getProductShippingClassesForSite()
                if (cachedShippingClasses.isEmpty()) {
                    productShippingClassViewState.copy(isLoadingProgressShown = true)
                } else {
                    productShippingClassViewState.copy(shippingClassList = cachedShippingClasses)
                }
            }

            // fetch shipping classes from the backend
            val shippingClasses = productRepository.fetchShippingClassesForSite(loadMore)
            productShippingClassViewState = productShippingClassViewState.copy(
                    isLoadingProgressShown = false,
                    isLoadingMoreProgressShown = false,
                    shippingClassList = shippingClasses
            )
        }
    }

    /**
     * If shipping classes are already being fetch, wait for the current fetch to complete - this is
     * used above to avoid fetching multiple pages of shipping classes in unison
     */
    private fun waitForExistingShippingClassFetch() {
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

    fun onShippingClassClicked(shippingClass: ShippingClass) {
        triggerEvent(ExitWithResult(shippingClass))
    }

    @Parcelize
    data class ProductShippingClassViewState(
        val isLoadingProgressShown: Boolean = false,
        val isLoadingMoreProgressShown: Boolean = false,
        val shippingClassList: List<ShippingClass>? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductShippingClassViewModel>
}
