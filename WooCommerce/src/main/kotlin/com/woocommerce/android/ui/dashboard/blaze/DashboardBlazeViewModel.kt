package com.woocommerce.android.ui.dashboard.blaze

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
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
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetAction
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.blaze.DashboardBlazeViewModel.DashboardBlazeCampaignState.Campaign
import com.woocommerce.android.ui.dashboard.blaze.DashboardBlazeViewModel.DashboardBlazeCampaignState.Hidden
import com.woocommerce.android.ui.dashboard.blaze.DashboardBlazeViewModel.DashboardBlazeCampaignState.NoCampaign
import com.woocommerce.android.ui.dashboard.data.DashboardRepository
import com.woocommerce.android.ui.products.ProductListRepository
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting
import javax.inject.Inject

@HiltViewModel
class DashboardBlazeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeMostRecentBlazeCampaign: ObserveMostRecentBlazeCampaign,
    private val productListRepository: ProductListRepository,
    private val isBlazeEnabled: IsBlazeEnabled,
    private val blazeUrlsHelper: BlazeUrlsHelper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val dashboardRepository: DashboardRepository,
    private val prefsWrapper: AppPrefsWrapper
) : ScopedViewModel(savedStateHandle) {
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

    private val hideWidgetAction = DashboardWidgetAction(
        titleResource = string.dynamic_dashboard_hide_widget_menu_item,
        action = { onBlazeViewDismissed() }
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
            menu = DashboardWidgetMenu(
                items = listOf(hideWidgetAction)
            ),
            createCampaignButton = DashboardWidgetAction(
                titleResource = string.blaze_campaign_promote_button,
                action = {
                    launchCampaignCreation(if (products.size == 1) product.remoteId else null)
                }
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
            onViewAllCampaignsClicked = {
                viewAllCampaigns()
            },
            menu = DashboardWidgetMenu(
                items = listOf(
                    hideWidgetAction,
                    DashboardWidgetAction(
                        titleResource = string.blaze_campaign_show_all_button,
                        action = { triggerEvent(ShowAllCampaigns) }
                    )
                )
            ),
            createCampaignButton = DashboardWidgetAction(
                titleResource = string.blaze_campaign_promote_button,
                action = {
                    launchCampaignCreation(productId = null)
                }
            )
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
        triggerEvent(LaunchBlazeCampaignCreation(productId))
    }

    fun onBlazeViewDismissed() {
        if (FeatureFlag.DYNAMIC_DASHBOARD.isEnabled()) {
            viewModelScope.launch {
                dashboardRepository.updateWidgetVisibility(type = DashboardWidget.Type.BLAZE, isVisible = false)
            }
        } else {
            prefsWrapper.isMyStoreBlazeViewDismissed = true
        }
        analyticsTrackerWrapper.track(
            stat = BLAZE_VIEW_DISMISSED,
            properties = mapOf(
                AnalyticsTracker.KEY_BLAZE_SOURCE to MY_STORE_SECTION.trackingName
            )
        )
    }

    sealed class DashboardBlazeCampaignState(
        open val menu: DashboardWidgetMenu,
        open val createCampaignButton: DashboardWidgetAction? = null
    ) {
        data object Hidden : DashboardBlazeCampaignState(DashboardWidgetMenu(emptyList()))
        data class NoCampaign(
            val product: BlazeProductUi,
            val onProductClicked: () -> Unit,
            override val menu: DashboardWidgetMenu,
            override val createCampaignButton: DashboardWidgetAction
        ) : DashboardBlazeCampaignState(menu, createCampaignButton)

        data class Campaign(
            val campaign: BlazeCampaignUi,
            val onCampaignClicked: () -> Unit,
            val onViewAllCampaignsClicked: () -> Unit,
            override val menu: DashboardWidgetMenu,
            override val createCampaignButton: DashboardWidgetAction
        ) : DashboardBlazeCampaignState(menu, createCampaignButton)
    }

    data class LaunchBlazeCampaignCreation(val productId: Long?) : MultiLiveEvent.Event()

    object ShowAllCampaigns : MultiLiveEvent.Event()
    data class ShowCampaignDetails(
        val url: String,
        val urlToTriggerExit: String
    ) : MultiLiveEvent.Event()
}
