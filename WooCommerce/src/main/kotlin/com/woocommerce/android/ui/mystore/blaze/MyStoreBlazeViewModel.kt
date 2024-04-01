package com.woocommerce.android.ui.mystore.blaze

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_CAMPAIGN_DETAIL_SELECTED
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_CAMPAIGN_LIST_ENTRY_POINT_SELECTED
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_ENTRY_POINT_DISPLAYED
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_VIEW_DISMISSED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.blaze.BlazeCampaignStat
import com.woocommerce.android.ui.blaze.BlazeCampaignUi
import com.woocommerce.android.ui.blaze.BlazeProductUi
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource.MY_STORE_SECTION
import com.woocommerce.android.ui.blaze.CampaignStatusUi
import com.woocommerce.android.ui.blaze.IsBlazeEnabled
import com.woocommerce.android.ui.blaze.ObserveMostRecentBlazeCampaign
import com.woocommerce.android.ui.mystore.blaze.MyStoreBlazeViewModel.MyStoreBlazeCampaignState.Campaign
import com.woocommerce.android.ui.mystore.blaze.MyStoreBlazeViewModel.MyStoreBlazeCampaignState.Hidden
import com.woocommerce.android.ui.mystore.blaze.MyStoreBlazeViewModel.MyStoreBlazeCampaignState.NoCampaign
import com.woocommerce.android.ui.products.ProductListRepository
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting
import javax.inject.Inject

@HiltViewModel
class MyStoreBlazeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeMostRecentBlazeCampaign: ObserveMostRecentBlazeCampaign,
    private val productListRepository: ProductListRepository,
    private val isBlazeEnabled: IsBlazeEnabled,
    private val blazeUrlsHelper: BlazeUrlsHelper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val prefsWrapper: AppPrefsWrapper
) : ScopedViewModel(savedStateHandle) {
    private val blazeCampaignState: Flow<MyStoreBlazeCampaignState> = flow {
        if (!isBlazeEnabled()) {
            emit(Hidden)
        } else {
            analyticsTrackerWrapper.track(
                stat = BLAZE_ENTRY_POINT_DISPLAYED,
                properties = mapOf(
                    AnalyticsTracker.KEY_BLAZE_SOURCE to BlazeFlowSource.MY_STORE_SECTION.trackingName
                )
            )

            emitAll(
                combine(
                    observeMostRecentBlazeCampaign(),
                    getProductsFlow()
                ) { blazeCampaignModel, products ->
                    when {
                        products.isEmpty() -> Hidden
                        blazeCampaignModel == null -> showUiForNoCampaign(products)
                        else -> showUiForCampaign(blazeCampaignModel)
                    }
                }
            )
        }
    }

    private val isBlazeDismissed = prefsWrapper.observePrefs()
        .onStart { emit(Unit) }
        .map { prefsWrapper.isMyStoreBlazeViewDismissed }

    val blazeViewState = combine(
        blazeCampaignState,
        isBlazeDismissed
    ) { blazeViewState, isBlazeDismissed ->
        if (isBlazeDismissed) Hidden else blazeViewState
    }.asLiveData()

    private fun showUiForNoCampaign(products: List<Product>): MyStoreBlazeCampaignState {
        val product = products.first()
        return NoCampaign(
            product = BlazeProductUi(
                name = product.name,
                imgUrl = product.firstImageUrl.orEmpty(),
            ),
            onProductClicked = {
                launchCampaignCreation(product.remoteId)
            },
            onCreateCampaignClicked = {
                launchCampaignCreation(if (products.size == 1) product.remoteId else null)
            }
        )
    }

    private fun showUiForCampaign(campaign: BlazeCampaignModel): MyStoreBlazeCampaignState {
        return Campaign(
            campaign = BlazeCampaignUi(
                product = BlazeProductUi(
                    name = campaign.title,
                    imgUrl = campaign.imageUrl.orEmpty(),
                ),
                status = CampaignStatusUi.fromString(campaign.uiStatus),
                stats = listOf(
                    BlazeCampaignStat(
                        name = string.blaze_campaign_status_impressions,
                        value = campaign.impressions.toString()
                    ),
                    BlazeCampaignStat(
                        name = string.blaze_campaign_status_clicks,
                        value = campaign.clicks.toString()
                    )
                )
            ),
            onCampaignClicked = {
                analyticsTrackerWrapper.track(
                    stat = BLAZE_CAMPAIGN_DETAIL_SELECTED,
                    properties = mapOf(
                        AnalyticsTracker.KEY_BLAZE_SOURCE to MY_STORE_SECTION.trackingName
                    )
                )
                triggerEvent(
                    ShowCampaignDetails(
                        url = blazeUrlsHelper.buildCampaignDetailsUrl(campaign.campaignId),
                        urlToTriggerExit = blazeUrlsHelper.buildCampaignsListUrl()
                    )
                )
            },
            onViewAllCampaignsClicked = {
                analyticsTrackerWrapper.track(
                    stat = BLAZE_CAMPAIGN_LIST_ENTRY_POINT_SELECTED,
                    properties = mapOf(
                        AnalyticsTracker.KEY_BLAZE_SOURCE to MY_STORE_SECTION.trackingName
                    )
                )
                triggerEvent(ShowAllCampaigns)
            },
            onCreateCampaignClicked = {
                launchCampaignCreation(productId = null)
            }
        )
    }

    private fun getProductsFlow(): Flow<List<Product>> {
        fun getCachedProducts() = productListRepository.getProductList(
            productFilterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value),
            sortType = ProductSorting.DATE_DESC,
        ).filterNot { it.isSampleProduct }
        return flow {
            emit(getCachedProducts())
            refreshProducts()
            emit(getCachedProducts())
        }
    }

    private suspend fun refreshProducts() {
        productListRepository.fetchProductList(
            productFilterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value),
            sortType = ProductSorting.DATE_DESC,
        )
    }

    private fun launchCampaignCreation(productId: Long?) {
        if (FeatureFlag.BLAZE_I3.isEnabled()) {
            triggerEvent(LaunchBlazeCampaignCreation(productId))
        } else {
            val source = BlazeFlowSource.MY_STORE_SECTION
            val url = if (productId != null) {
                blazeUrlsHelper.buildUrlForProduct(productId, source)
            } else {
                blazeUrlsHelper.buildUrlForSite(source)
            }
            triggerEvent(
                LaunchBlazeCampaignCreationUsingWebView(url = url, source = source)
            )
        }
    }

    fun onBlazeViewDismissed() {
        prefsWrapper.isMyStoreBlazeViewDismissed = true
        analyticsTrackerWrapper.track(
            stat = BLAZE_VIEW_DISMISSED,
            properties = mapOf(
                AnalyticsTracker.KEY_BLAZE_SOURCE to BlazeFlowSource.MY_STORE_SECTION.trackingName
            )
        )
    }

    sealed interface MyStoreBlazeCampaignState {
        object Hidden : MyStoreBlazeCampaignState
        data class NoCampaign(
            val product: BlazeProductUi,
            val onProductClicked: () -> Unit,
            val onCreateCampaignClicked: () -> Unit,
        ) : MyStoreBlazeCampaignState

        data class Campaign(
            val campaign: BlazeCampaignUi,
            val onCampaignClicked: () -> Unit,
            val onViewAllCampaignsClicked: () -> Unit,
            val onCreateCampaignClicked: () -> Unit,
        ) : MyStoreBlazeCampaignState
    }

    data class LaunchBlazeCampaignCreationUsingWebView(
        val url: String,
        val source: BlazeFlowSource
    ) : MultiLiveEvent.Event()

    data class LaunchBlazeCampaignCreation(val productId: Long?) : MultiLiveEvent.Event()

    object ShowAllCampaigns : MultiLiveEvent.Event()
    data class ShowCampaignDetails(
        val url: String,
        val urlToTriggerExit: String
    ) : MultiLiveEvent.Event()
}
