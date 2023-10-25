package com.woocommerce.android.ui.blaze

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.products.ProductListRepository
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    private val blazeUrlsHelper: BlazeUrlsHelper
) : ScopedViewModel(savedStateHandle) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val blazeCampaignState = flow {
        if (!isBlazeEnabled()) emit(MyStoreBlazeCampaignState.Hidden)
        else {
            emitAll(
                observeMostRecentBlazeCampaign().flatMapLatest {
                    when (it) {
                        null -> prepareUiForNoCampaign()
                        else -> prepareUiForCampaign(it)
                    }
                }
            )
        }
    }.asLiveData()

    private fun prepareUiForNoCampaign(): Flow<MyStoreBlazeCampaignState> {
        fun launchCampaignCreation(productId: Long?) {
            val url = if (productId != null) {
                blazeUrlsHelper.buildUrlForProduct(productId, BlazeFlowSource.MY_STORE_BANNER)
            } else {
                blazeUrlsHelper.buildUrlForSite(BlazeFlowSource.MY_STORE_BANNER)
            }
            triggerEvent(LaunchBlazeCampaignCreation(url))
        }

        return getProducts().map { products ->
            val product = products.firstOrNull() ?: return@map MyStoreBlazeCampaignState.Hidden
            MyStoreBlazeCampaignState.NoCampaign(
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
    }

    private fun prepareUiForCampaign(campaign: BlazeCampaignModel): Flow<MyStoreBlazeCampaignState> {
        return flowOf(
            MyStoreBlazeCampaignState.Campaign(
                campaign = BlazeCampaignUi(
                    product = BlazeProductUi(
                        name = campaign.title,
                        imgUrl = campaign.imageUrl.orEmpty(),
                    ),
                    status = CampaignStatusUi.fromString(campaign.uiStatus),
                    stats = listOf(
                        BlazeCampaignStat(
                            name = R.string.blaze_campaign_status_impressions,
                            value = campaign.impressions
                        ),
                        BlazeCampaignStat(
                            name = R.string.blaze_campaign_status_clicks,
                            value = campaign.clicks
                        )
                    )
                ),
                onCampaignClicked = {
                    triggerEvent(
                        ShowCampaignDetails(
                            url = blazeUrlsHelper.buildCampaignDetailsUrl(campaign.campaignId),
                            urlToTriggerExit = blazeUrlsHelper.buildCampaignsListUrl()
                        )
                    )
                },
                onViewAllCampaignsClicked = {
                    triggerEvent(ShowAllCampaigns)
                },
                onCreateCampaignClicked = {
                    triggerEvent(
                        LaunchBlazeCampaignCreation(blazeUrlsHelper.buildUrlForSite(BlazeFlowSource.MY_STORE_BANNER))
                    )
                }
            )
        )
    }

    private fun getProducts(): Flow<List<Product>> {
        fun getCachedProducts() = productListRepository.getProductList(
            productFilterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value),
            sortType = ProductSorting.DATE_DESC,
        ).filterNot { it.isSampleProduct }
        return flow {
            emit(getCachedProducts())
            productListRepository.fetchProductList(
                productFilterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value),
                sortType = ProductSorting.DATE_DESC,
            )
            emit(getCachedProducts())
        }
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

    data class LaunchBlazeCampaignCreation(val url: String) : MultiLiveEvent.Event()
    object ShowAllCampaigns : MultiLiveEvent.Event()
    data class ShowCampaignDetails(
        val url: String,
        val urlToTriggerExit: String
    ) : MultiLiveEvent.Event()
}
