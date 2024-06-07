package com.woocommerce.android.ui.dashboard.blaze

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_CAMPAIGN_DETAIL_SELECTED
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_CAMPAIGN_LIST_ENTRY_POINT_SELECTED
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_ENTRY_POINT_DISPLAYED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.blaze.BlazeCampaignStat
import com.woocommerce.android.ui.blaze.BlazeCampaignUi
import com.woocommerce.android.ui.blaze.BlazeProductUi
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource.MY_STORE_SECTION
import com.woocommerce.android.ui.blaze.CampaignStatusUi
import com.woocommerce.android.ui.blaze.ObserveMostRecentBlazeCampaign
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetAction
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.DashboardViewModel.RefreshEvent
import com.woocommerce.android.ui.dashboard.blaze.DashboardBlazeViewModel.DashboardBlazeCampaignState.Campaign
import com.woocommerce.android.ui.dashboard.blaze.DashboardBlazeViewModel.DashboardBlazeCampaignState.NoCampaign
import com.woocommerce.android.ui.dashboard.defaultHideMenuEntry
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting

@HiltViewModel(assistedFactory = DashboardBlazeViewModel.Factory::class)
@Suppress("LongParameterList")
class DashboardBlazeViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    @Assisted private val parentViewModel: DashboardViewModel,
    observeMostRecentBlazeCampaign: ObserveMostRecentBlazeCampaign,
    private val productListRepository: ProductListRepository,
    private val blazeUrlsHelper: BlazeUrlsHelper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    private val _refreshTrigger = MutableSharedFlow<RefreshEvent>(extraBufferCapacity = 1)
    private val refreshTrigger = merge(_refreshTrigger, (parentViewModel.refreshTrigger))
        .onStart { emit(RefreshEvent()) }

    @OptIn(ExperimentalCoroutinesApi::class)
    val blazeViewState = flow {
        analyticsTrackerWrapper.track(
            stat = BLAZE_ENTRY_POINT_DISPLAYED,
            properties = mapOf(
                AnalyticsTracker.KEY_BLAZE_SOURCE to MY_STORE_SECTION.trackingName
            )
        )

        emitAll(
            refreshTrigger.flatMapLatest { refreshEvent ->
                combine(
                    observeMostRecentBlazeCampaign(forceRefresh = refreshEvent.isForced),
                    getProductsFlow(forceRefresh = refreshEvent.isForced)
                ) { blazeCampaignResult, productsResult ->
                    if (productsResult.isFailure || blazeCampaignResult.isFailure) {
                        return@combine DashboardBlazeCampaignState.Error(widgetMenu)
                    }
                    val products = productsResult.getOrThrow()
                    val blazeCampaignModel = blazeCampaignResult.getOrThrow()

                    if (products.isEmpty()) {
                        // When the products is empty, the card will be hidden by the parent view model
                        // so we don't need to show any UI
                        return@combine null
                    }

                    when {
                        blazeCampaignModel == null -> showUiForNoCampaign(products)
                        else -> showUiForCampaign(blazeCampaignModel)
                    }
                }.onStart {
                    emit(DashboardBlazeCampaignState.Loading)
                }
            }
        )
    }.asLiveData()

    private val widgetMenu = DashboardWidgetMenu(
        items = listOf(
            DashboardWidget.Type.BLAZE.defaultHideMenuEntry {
                parentViewModel.onHideWidgetClicked(DashboardWidget.Type.BLAZE)
            }
        )
    )

    private fun showUiForNoCampaign(products: List<Product>): DashboardBlazeCampaignState {
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
            },
            menu = widgetMenu
        )
    }

    private fun showUiForCampaign(campaign: BlazeCampaignModel): DashboardBlazeCampaignState {
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
                parentViewModel.trackCardInteracted(DashboardWidget.Type.BLAZE.trackingIdentifier)
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
            onCreateCampaignClicked = {
                launchCampaignCreation(productId = null)
            },
            menu = widgetMenu,
            showAllCampaignsButton = DashboardWidgetAction(
                titleResource = string.blaze_campaign_show_all_button,
                action = { viewAllCampaigns() }
            ),
        )
    }

    private fun viewAllCampaigns() {
        parentViewModel.trackCardInteracted(DashboardWidget.Type.BLAZE.trackingIdentifier)
        analyticsTrackerWrapper.track(
            stat = BLAZE_CAMPAIGN_LIST_ENTRY_POINT_SELECTED,
            properties = mapOf(
                AnalyticsTracker.KEY_BLAZE_SOURCE to MY_STORE_SECTION.trackingName
            )
        )
        triggerEvent(ShowAllCampaigns)
    }

    private fun getProductsFlow(forceRefresh: Boolean): Flow<Result<List<Product>>> {
        return flow {
            if (forceRefresh) {
                refreshProducts().onFailure {
                    emit(Result.failure(it))
                    return@flow
                }
            }

            emitAll(
                productListRepository.observeProducts(
                    filterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value),
                    sortType = ProductSorting.DATE_DESC,
                    excludeSampleProducts = true,
                    // For optimization, load only 2 products, as we need only the first one, and
                    // and to check if there are more than 1 product to show the "Create Campaign" button
                    limit = 2
                ).map {
                    Result.success(it)
                }
            )
        }
    }

    private suspend fun refreshProducts() = productListRepository.fetchProductList(
        productFilterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value),
        sortType = ProductSorting.DATE_DESC,
    )

    private fun launchCampaignCreation(productId: Long?) {
        parentViewModel.trackCardInteracted(DashboardWidget.Type.BLAZE.trackingIdentifier)
        triggerEvent(LaunchBlazeCampaignCreation(productId))
    }

    fun onRefresh() {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.DYNAMIC_DASHBOARD_CARD_RETRY_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_TYPE to DashboardWidget.Type.BLAZE.trackingIdentifier
            )
        )
        _refreshTrigger.tryEmit(RefreshEvent(isForced = true))
    }

    sealed class DashboardBlazeCampaignState(
        open val menu: DashboardWidgetMenu,
        val mainButton: DashboardWidgetAction? = null
    ) {
        data object Loading : DashboardBlazeCampaignState(DashboardWidgetMenu(emptyList()))
        data class Error(
            override val menu: DashboardWidgetMenu
        ) : DashboardBlazeCampaignState(menu)

        data class NoCampaign(
            val product: BlazeProductUi,
            val onProductClicked: () -> Unit,
            val onCreateCampaignClicked: () -> Unit,
            override val menu: DashboardWidgetMenu,
        ) : DashboardBlazeCampaignState(menu)

        data class Campaign(
            val campaign: BlazeCampaignUi,
            val onCampaignClicked: () -> Unit,
            val onCreateCampaignClicked: () -> Unit,
            val showAllCampaignsButton: DashboardWidgetAction,
            override val menu: DashboardWidgetMenu
        ) : DashboardBlazeCampaignState(menu, showAllCampaignsButton)
    }

    data class LaunchBlazeCampaignCreation(val productId: Long?) : MultiLiveEvent.Event()

    object ShowAllCampaigns : MultiLiveEvent.Event()
    data class ShowCampaignDetails(
        val url: String,
        val urlToTriggerExit: String
    ) : MultiLiveEvent.Event()

    @AssistedFactory
    interface Factory {
        fun create(parentViewModel: DashboardViewModel?): DashboardBlazeViewModel
    }
}
