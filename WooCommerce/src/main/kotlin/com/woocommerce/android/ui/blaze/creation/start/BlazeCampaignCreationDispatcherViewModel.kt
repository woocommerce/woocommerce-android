package com.woocommerce.android.ui.blaze.creation.start

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.products.ProductListRepository
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignCreationDispatcherViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val blazeRepository: BlazeRepository,
    private val productListRepository: ProductListRepository
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeCampaignCreationDispatcherFragmentArgs by savedStateHandle.navArgs()
    init {
        launch {
            when {
                blazeRepository.getMostRecentCampaign() == null -> showIntro()
                else -> startCampaignCreationWithoutIntro()
            }
        }
    }

    fun onProductSelected(productId: Long) {
        triggerEvent(ShowBlazeCampaignCreationAdForm(productId))
    }

    private fun showIntro() {
        triggerEvent(ShowBlazeCampaignCreationIntro(navArgs.productId))
    }

    private suspend fun startCampaignCreationWithoutIntro() {
        val products = getPublishedProducts()

        when {
            navArgs.productId != -1L -> triggerEvent(ShowBlazeCampaignCreationAdForm(navArgs.productId))
            products.size == 1 -> triggerEvent(ShowBlazeCampaignCreationAdForm(products.first().remoteId))
            products.isNotEmpty() -> triggerEvent(ShowProductSelectorScreen)
            else -> {
                WooLog.w(WooLog.T.BLAZE, "No products available to create a campaign")
                triggerEvent(Exit)
            }
        }
    }

    private fun getPublishedProducts() = productListRepository.getProductList(
        productFilterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value),
        sortType = ProductSorting.DATE_DESC,
    ).filterNot { it.isSampleProduct }

    data class ShowBlazeCampaignCreationIntro(
        val productId: Long
    ) : MultiLiveEvent.Event()
    data class ShowBlazeCampaignCreationAdForm(
        val productId: Long
    ) : MultiLiveEvent.Event()
    object ShowProductSelectorScreen : MultiLiveEvent.Event()
}
