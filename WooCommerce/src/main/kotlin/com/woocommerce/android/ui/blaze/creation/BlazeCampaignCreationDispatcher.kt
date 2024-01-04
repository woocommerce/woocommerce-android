package com.woocommerce.android.ui.blaze.creation

import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.creation.intro.BlazeCampaignCreationIntroFragmentArgs
import com.woocommerce.android.ui.products.ProductListRepository
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.selector.ProductSelectorFragment
import com.woocommerce.android.ui.products.selector.ProductSelectorFragmentArgs
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.ProductSelectorFlow.Undefined
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.SelectedItem
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.SelectionHandling.SIMPLE
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.SelectionMode.SINGLE
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting
import java.lang.ref.WeakReference
import javax.inject.Inject

class BlazeCampaignCreationDispatcher @Inject constructor(
    private val blazeRepository: BlazeRepository,
    private val productListRepository: ProductListRepository,
    private val coroutineDispatchers: CoroutineDispatchers
) {
    private var fragmentReference: WeakReference<BaseFragment> = WeakReference(null)

    fun attachFragment(fragment: BaseFragment) {
        this.fragmentReference = WeakReference(fragment)
        fragment.handleResult<Collection<SelectedItem>>(ProductSelectorFragment.PRODUCT_SELECTOR_RESULT) {
            this.fragmentReference.get()?.showCampaignForm(it.first().id)
        }
    }

    suspend fun startCampaignCreation(
        productId: Long? = null,
        handler: (BlazeCampaignCreationDispatcherEvent) -> Unit = ::handleEvent
    ) {
        when {
            blazeRepository.getMostRecentCampaign() == null -> handler(
                BlazeCampaignCreationDispatcherEvent.ShowBlazeCampaignCreationIntro(productId)
            )

            else -> startCampaignCreationWithoutIntro(productId, handler)
        }
    }

    private suspend fun startCampaignCreationWithoutIntro(
        productId: Long?,
        handler: (BlazeCampaignCreationDispatcherEvent) -> Unit
    ) {
        val products = getPublishedCachedProducts()

        when {
            productId != null -> {
                handler(BlazeCampaignCreationDispatcherEvent.ShowBlazeCampaignCreationForm(productId))
            }
            products.size == 1 -> {
                handler(BlazeCampaignCreationDispatcherEvent.ShowBlazeCampaignCreationForm(products.first().remoteId))
            }
            products.isNotEmpty() -> {
                handler(BlazeCampaignCreationDispatcherEvent.ShowProductSelectorScreen)
            }
            else -> {
                WooLog.w(WooLog.T.BLAZE, "Fetching products from the API")
                val fetchedProducts = productListRepository.fetchProductList()
                if (fetchedProducts.isNotEmpty()) {
                    handler(BlazeCampaignCreationDispatcherEvent.ShowProductSelectorScreen)
                } else {
                    WooLog.w(WooLog.T.BLAZE, "No products available to create a campaign")
                }
            }
        }
    }

    private suspend fun getPublishedCachedProducts() = withContext(coroutineDispatchers.io) {
        productListRepository.getProductList(
            productFilterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value),
            sortType = ProductSorting.DATE_DESC,
        ).filterNot { it.isSampleProduct }
    }

    private fun handleEvent(event: BlazeCampaignCreationDispatcherEvent) {
        when (event) {
            is BlazeCampaignCreationDispatcherEvent.ShowBlazeCampaignCreationIntro -> fragmentReference.get()
                ?.showIntro(event.productId)

            is BlazeCampaignCreationDispatcherEvent.ShowBlazeCampaignCreationForm -> fragmentReference.get()
                ?.showCampaignForm(event.productId)

            is BlazeCampaignCreationDispatcherEvent.ShowProductSelectorScreen -> fragmentReference.get()
                ?.showProductSelector()
        }
    }

    private fun BaseFragment.showIntro(productId: Long?) {
        findNavController().navigateToBlazeGraph(
            startDestination = R.id.blazeCampaignCreationIntroFragment,
            bundle = BlazeCampaignCreationIntroFragmentArgs(productId ?: -1L).toBundle()
        )
    }

    private fun BaseFragment.showCampaignForm(productId: Long) {
        // TODO update when the AD form is implemented
        Toast.makeText(
            requireContext(),
            "This will show the campaign creation form for product $productId",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun BaseFragment.showProductSelector() {
        val navGraph = findNavController().graph.findNode(R.id.nav_graph_blaze_campaign_creation) as NavGraph
        navGraph.setStartDestination(R.id.nav_graph_product_selector)

        findNavController().navigateToBlazeGraph(
            startDestination = R.id.nav_graph_product_selector,
            bundle = ProductSelectorFragmentArgs(
                selectionMode = SINGLE,
                selectionHandling = SIMPLE,
                screenTitleOverride = getString(string.blaze_campaign_creation_product_selector_title),
                ctaButtonTextOverride = getString(string.blaze_campaign_creation_product_selector_cta_button),
                productSelectorFlow = Undefined
            ).toBundle()
        )
    }

    private fun NavController.navigateToBlazeGraph(
        startDestination: Int,
        bundle: android.os.Bundle? = null,
    ) {
        val navGraph = graph.findNode(R.id.nav_graph_blaze_campaign_creation) as NavGraph
        navGraph.setStartDestination(startDestination)

        navigateSafely(
            resId = navGraph.id,
            bundle = bundle,
            navOptions = navOptions {
                anim {
                    enter = R.anim.default_enter_anim
                    exit = R.anim.default_exit_anim
                    popEnter = R.anim.default_pop_enter_anim
                    popExit = R.anim.default_pop_exit_anim
                }
            }
        )
    }

    sealed interface BlazeCampaignCreationDispatcherEvent {
        data class ShowBlazeCampaignCreationIntro(
            val productId: Long?
        ) : BlazeCampaignCreationDispatcherEvent

        data class ShowBlazeCampaignCreationForm(
            val productId: Long
        ) : BlazeCampaignCreationDispatcherEvent

        object ShowProductSelectorScreen : BlazeCampaignCreationDispatcherEvent
    }
}
