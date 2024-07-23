package com.woocommerce.android.ui.products.shipping

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.ShippingClass
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ProductShippingClassViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val productRepository: ProductShippingClassRepository,
    resourceProvider: ResourceProvider
) : ScopedViewModel(savedState) {
    private val noShippingClass = ShippingClass(
        name = resourceProvider.getString(R.string.product_no_shipping_class),
        slug = "",
        remoteShippingClassId = 0
    )
    private var shippingClassLoadJob: Job? = null

    // view state for the shipping class screen
    final /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    /**
     * Load & fetch the shipping classes for the current site, optionally performing a "load more" to
     * load the next page of shipping classes
     */
    fun loadShippingClasses(loadMore: Boolean = false) {
        if (loadMore && !productRepository.canLoadMoreShippingClasses) {
            WooLog.d(WooLog.T.PRODUCTS, "Can't load more product shipping classes")
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
                    WooLog.T.PRODUCTS,
                    "CancellationException while waiting for existing shipping class list fetch"
                )
            }
        }
    }

    fun onShippingClassClicked(shippingClass: ShippingClass) {
        triggerEvent(MultiLiveEvent.Event.ExitWithResult(shippingClass))
    }

    @Parcelize
    data class ViewState(
        val isLoadingProgressShown: Boolean = false,
        val isLoadingMoreProgressShown: Boolean = false,
        val shippingClassList: List<ShippingClass>? = null
    ) : Parcelable
}
