package com.woocommerce.android.ui.dashboard.blaze

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
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.blaze.BlazeCampaignStat
import com.woocommerce.android.ui.blaze.BlazeCampaignUi
import com.woocommerce.android.ui.blaze.BlazeProductUi
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource.MY_STORE_SECTION
import com.woocommerce.android.ui.blaze.CampaignStatusUi
import com.woocommerce.android.ui.blaze.IsBlazeEnabled
import com.woocommerce.android.ui.blaze.ObserveMostRecentBlazeCampaign
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetAction
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.DashboardViewModel.RefreshEvent
import com.woocommerce.android.ui.dashboard.blaze.DashboardBlazeViewModel.DashboardBlazeCampaignState.Campaign
import com.woocommerce.android.ui.dashboard.blaze.DashboardBlazeViewModel.DashboardBlazeCampaignState.Hidden
import com.woocommerce.android.ui.dashboard.blaze.DashboardBlazeViewModel.DashboardBlazeCampaignState.NoCampaign
import com.woocommerce.android.ui.dashboard.defaultHideMenuEntry
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting

@HiltViewModel(assistedFactory = DashboardBlazeViewModel.Factory::class)
@Suppress("LongParameterList")
class DashboardBlazeViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    // TODO make this non-nullable when enabling [FeatureFlag.DYNAMIC_DASHBOARD]
    @Assisted parentViewModel: DashboardViewModel?,
    observeMostRecentBlazeCampaign: ObserveMostRecentBlazeCampaign,
    private val productListRepository: ProductListRepository,
    private val isBlazeEnabled: IsBlazeEnabled,
    private val blazeUrlsHelper: BlazeUrlsHelper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val prefsWrapper: AppPrefsWrapper
) : ScopedViewModel(savedStateHandle) {
    private val refreshTrigger = (parentViewModel?.refreshTrigger ?: emptyFlow())
        .onStart { emit(RefreshEvent()) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val blazeCampaignState: Flow<DashboardBlazeCampaignState> = flow {
        if (!isBlazeEnabled()) {
            emit(Hidden)
        } else {
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
                    ) { blazeCampaignModel, products ->
                        when {
                            products.isEmpty() -> Hidden
                            blazeCampaignModel == null -> showUiForNoCampaign(products)
                            else -> showUiForCampaign(blazeCampaignModel)
                        }
                    }.onStart {
                        emit(DashboardBlazeCampaignState.Loading)
                    }
                }
            )
        }
    }

    private val isBlazeDismissed = if (FeatureFlag.DYNAMIC_DASHBOARD.isEnabled()) {
        flowOf(false)
    } else {
        prefsWrapper.observePrefs()
            .onStart { emit(Unit) }
            .map { prefsWrapper.isMyStoreBlazeViewDismissed }
            .distinctUntilChanged()
    }

    val blazeViewState = combine(
        blazeCampaignState,
        isBlazeDismissed
    ) { blazeViewState, isBlazeDismissed ->
        if (isBlazeDismissed) Hidden else blazeViewState
    }.asLiveData()

    private val hideWidgetAction = DashboardWidget.Type.BLAZE.defaultHideMenuEntry {
        parentViewModel?.onHideWidgetClicked(DashboardWidget.Type.BLAZE)
    }

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
            menu = DashboardWidgetMenu(
                items = listOf(hideWidgetAction)
            )
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
            menu = DashboardWidgetMenu(items = listOf(hideWidgetAction)),
            showAllCampaignsButton = DashboardWidgetAction(
                titleResource = string.blaze_campaign_show_all_button,
                action = { viewAllCampaigns() }
            ),
        )
    }

    private fun viewAllCampaigns() {
        analyticsTrackerWrapper.track(
            stat = BLAZE_CAMPAIGN_LIST_ENTRY_POINT_SELECTED,
            properties = mapOf(
                AnalyticsTracker.KEY_BLAZE_SOURCE to MY_STORE_SECTION.trackingName
            )
        )
        triggerEvent(ShowAllCampaigns)
    }

    private fun getProductsFlow(forceRefresh: Boolean): Flow<List<Product>> {
        return flow {
            if (forceRefresh) refreshProducts()

            emitAll(
                productListRepository.observeProducts(
                    filterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value),
                    sortType = ProductSorting.DATE_DESC,
                    excludeSampleProducts = true,
                    // For optimization, load only 2 products, as we need only the first one, and
                    // and to check if there are more than 1 product to show the "Create Campaign" button
                    limit = 2
                )
            )
        }
    }

    private suspend fun refreshProducts() {
        productListRepository.fetchProductList(
            productFilterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value),
            sortType = ProductSorting.DATE_DESC,
        )
    }

    private fun launchCampaignCreation(productId: Long?) {
        triggerEvent(LaunchBlazeCampaignCreation(productId))
    }

    fun onBlazeViewDismissed() {
        prefsWrapper.isMyStoreBlazeViewDismissed = true
        analyticsTrackerWrapper.track(
            stat = BLAZE_VIEW_DISMISSED,
            properties = mapOf(
                AnalyticsTracker.KEY_BLAZE_SOURCE to MY_STORE_SECTION.trackingName
            )
        )
    }

    sealed class DashboardBlazeCampaignState(
        open val menu: DashboardWidgetMenu,
        val mainButton: DashboardWidgetAction? = null
    ) {
        // TODO remove this state when enabling [FeatureFlag.DYNAMIC_DASHBOARD] and clean up the code
        data object Hidden : DashboardBlazeCampaignState(DashboardWidgetMenu(emptyList()))
        data object Loading : DashboardBlazeCampaignState(DashboardWidgetMenu(emptyList()))
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
