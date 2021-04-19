package com.woocommerce.android.ui.products

import android.os.Parcelable
import com.woocommerce.android.R
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ShippingClass
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class ProductShippingClassViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val productRepository: ProductShippingClassRepository,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedState, dispatchers) {
    private val noShippingClass = ShippingClass(
        name = resourceProvider.getString(R.string.product_no_shipping_class),
        slug = "",
        remoteShippingClassId = 0
    )
    private var shippingClassLoadJob: Job? = null

    // view state for the shipping class screen
    final val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    /**
     * Load & fetch the shipping classes for the current site, optionally performing a "load more" to
     * load the next page of shipping classes
     */
    fun loadShippingClasses(loadMore: Boolean = false) {
        if (loadMore && !productRepository.canLoadMoreShippingClasses) {
            WooLog.d(T.PRODUCTS, "Can't load more product shipping classes")
            return
        }

        launch {
            waitForExistingShippingClassFetch()
            shippingClassLoadJob = coroutineContext[Job]

            if (loadMore) {
                viewState = viewState.copy(isLoadingMoreProgressShown = true)
            } else {
                // get cached shipping classes and only show loading progress the list is empty, otherwise show
                // them right away
                val cachedShippingClasses = productRepository.getProductShippingClassesForSite()
                if (cachedShippingClasses.isEmpty()) {
                    viewState = viewState.copy(isLoadingProgressShown = true)
                } else {
                    updateShippingClasses(cachedShippingClasses)
                }
            }

            // fetch shipping classes from the backend
            val shippingClasses = productRepository.fetchShippingClassesForSite(loadMore)
            updateShippingClasses(shippingClasses)

            viewState = viewState.copy(
                isLoadingProgressShown = false,
                isLoadingMoreProgressShown = false
            )
        }
    }

    private fun updateShippingClasses(shippingClasses: List<ShippingClass>) {
        viewState = viewState.copy(
            shippingClassList = listOf(noShippingClass) + shippingClasses
        )
    }

    /**
     * If shipping classes are already being fetched, wait for the current fetch to complete - this is
     * used above to avoid fetching multiple pages of shipping classes in unison
     */
    private suspend fun waitForExistingShippingClassFetch() {
        if (shippingClassLoadJob?.isActive == true) {
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

    fun onShippingClassClicked(shippingClass: ShippingClass) {
        triggerEvent(ExitWithResult(shippingClass))
    }

    @Parcelize
    data class ViewState(
        val isLoadingProgressShown: Boolean = false,
        val isLoadingMoreProgressShown: Boolean = false,
        val shippingClassList: List<ShippingClass>? = null
    ) : Parcelable

    @AssistedFactory
    interface Factory : ViewModelAssistedFactory<ProductShippingClassViewModel>
}
