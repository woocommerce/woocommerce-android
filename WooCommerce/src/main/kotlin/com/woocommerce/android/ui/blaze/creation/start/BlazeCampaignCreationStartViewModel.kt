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
class BlazeCampaignCreationStartViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val blazeRepository: BlazeRepository,
    private val productListRepository: ProductListRepository
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeCampaignCreationStartFragmentArgs by savedStateHandle.navArgs()
    init {
        launch {
            when {
                blazeRepository.getMostRecentCampaign() == null -> {
                    triggerEvent(ShowBlazeCampaignCreationIntro(navArgs.productId))
                }
                else -> startCampaignCreationWithoutIntro()
            }
        }
    }

    fun onProductSelected(productId: Long) = launch {
        loadCampaignDefaults(productId)
    }

    private suspend fun startCampaignCreationWithoutIntro() {
        val products = getPublishedProducts()

        when {
            navArgs.productId != -1L -> loadCampaignDefaults(navArgs.productId)
            products.size == 1 -> loadCampaignDefaults(products.first().remoteId)
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

    @Suppress("UNUSED_PARAMETER", "RedundantSuspendModifier")
    private suspend fun loadCampaignDefaults(productId: Long) {
        TODO("Make call to the AI to generate the campaign defaults and then navigate to the AD preview")
    }

    data class ShowBlazeCampaignCreationIntro(
        val productId: Long
    ) : MultiLiveEvent.Event()
    object ShowProductSelectorScreen : MultiLiveEvent.Event()
}
