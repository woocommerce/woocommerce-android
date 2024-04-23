package com.woocommerce.android.ui.blaze.creation.intro

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_ENTRY_POINT_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_INTRO_DISPLAYED
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_INTRO_LEARN_MORE_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignCreationIntroViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productListRepository: ProductListRepository,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeCampaignCreationIntroFragmentArgs by savedStateHandle.navArgs()
    fun onContinueClick() {
        suspend fun getPublishedProducts() = withContext(coroutineDispatchers.io) {
            analyticsTracker.track(
                stat = BLAZE_ENTRY_POINT_TAPPED,
                properties = mapOf(AnalyticsTracker.KEY_BLAZE_SOURCE to BlazeFlowSource.INTRO_VIEW.trackingName)
            )
            productListRepository.getProductList(
                productFilterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value),
                sortType = ProductSorting.DATE_DESC,
            ).filterNot { it.isSampleProduct }
        }

        launch {
            if (navArgs.productId != -1L) {
                triggerEvent(ShowCampaignCreationForm(navArgs.productId, BlazeFlowSource.INTRO_VIEW))
            } else {
                val products = getPublishedProducts()
                when {
                    products.size == 1 -> triggerEvent(
                        ShowCampaignCreationForm(
                            products.first().remoteId,
                            BlazeFlowSource.INTRO_VIEW
                        )
                    )

                    products.isNotEmpty() -> triggerEvent(ShowProductSelector)
                    else -> {
                        WooLog.w(WooLog.T.BLAZE, "No products available to create a campaign")
                        triggerEvent(Exit)
                    }
                }
            }
        }
    }

    init {
        analyticsTracker.track(
            stat = BLAZE_INTRO_DISPLAYED,
            properties = mapOf(AnalyticsTracker.KEY_BLAZE_SOURCE to navArgs.source.trackingName)
        )
    }

    fun onDismissClick() {
        triggerEvent(Exit)
    }

    fun onProductSelected(productId: Long) {
        triggerEvent(ShowCampaignCreationForm(productId, BlazeFlowSource.INTRO_VIEW))
    }

    fun onLearnMoreClick() {
        analyticsTracker.track(stat = BLAZE_INTRO_LEARN_MORE_TAPPED)
    }

    object ShowProductSelector : MultiLiveEvent.Event()
    data class ShowCampaignCreationForm(val productId: Long, val source: BlazeFlowSource) : MultiLiveEvent.Event()
}
