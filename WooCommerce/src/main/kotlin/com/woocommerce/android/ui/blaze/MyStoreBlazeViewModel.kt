package com.woocommerce.android.ui.blaze

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.blaze.IsBlazeEnabled.BlazeFlowSource
import com.woocommerce.android.ui.products.ProductListRepository
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.util.FeatureFlag
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
    private val isBlazeEnabled: IsBlazeEnabled
) : ScopedViewModel(savedStateHandle) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val blazeCampaignState = flow {
        if (!FeatureFlag.BLAZE_ITERATION_2.isEnabled()) emit(MyStoreBlazeCampaignState.Hidden)
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
        return getProducts().map { products ->
            val product = products.firstOrNull() ?: return@map MyStoreBlazeCampaignState.Hidden
            MyStoreBlazeCampaignState.NoCampaign(
                product = BlazeProductUi(
                    name = product.name,
                    imgUrl = product.firstImageUrl.orEmpty(),
                ),
                onCreateCampaignClicked = {
                    val url = if (products.size == 1) {
                        isBlazeEnabled.buildUrlForProduct(product.remoteId, BlazeFlowSource.MY_STORE_BANNER)
                    } else {
                        isBlazeEnabled.buildUrlForSite(BlazeFlowSource.MY_STORE_BANNER)
                    }
                    triggerEvent(LaunchBlazeCampaignCreation(url, BlazeFlowSource.MY_STORE_BANNER))
                }
            )
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun prepareUiForCampaign(campaign: BlazeCampaignModel): Flow<MyStoreBlazeCampaignState> {
        return flowOf(
            MyStoreBlazeCampaignState.Campaign(
                campaign = BlazeCampaignUi(
                    product = BlazeProductUi(
                        name = "Product name",
                        imgUrl = "https://hips.hearstapps.com/hmg-prod/images/gh-082420-ghi-best-sofas-1598293488.png",
                    ),
                    status = CampaignStatusUi.Active,
                    impressions = 100,
                    clicks = 10,
                    budget = 1000
                ),
                onCampaignClicked = { /* TODO */ },
                onViewAllCampaignsClicked = { /* TODO */ },
                onCreateCampaignClicked = { /* TODO */ }
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
            val onCreateCampaignClicked: () -> Unit,
        ) : MyStoreBlazeCampaignState

        data class Campaign(
            val campaign: BlazeCampaignUi,
            val onCampaignClicked: () -> Unit,
            val onViewAllCampaignsClicked: () -> Unit,
            val onCreateCampaignClicked: () -> Unit,
        ) : MyStoreBlazeCampaignState
    }

    data class BlazeProductUi(
        val name: String,
        val imgUrl: String
    )

    data class BlazeCampaignUi(
        val product: BlazeProductUi,
        val status: CampaignStatusUi,
        val impressions: Int,
        val clicks: Int,
        val budget: Int
    )

    enum class CampaignStatusUi(
        @StringRes val statusDisplayText: Int,
        @ColorRes val textColor: Int,
        @ColorRes val backgroundColor: Int,
    ) {
        InModeration(
            statusDisplayText = R.string.blaze_campaign_status_in_moderation,
            textColor = R.color.blaze_campaign_status_in_moderation_text,
            backgroundColor = R.color.blaze_campaign_status_in_moderation_background
        ),
        Active(
            statusDisplayText = R.string.blaze_campaign_status_active,
            textColor = R.color.blaze_campaign_status_active_text,
            backgroundColor = R.color.blaze_campaign_status_active_background
        ),
        Completed(
            statusDisplayText = R.string.blaze_campaign_status_completed,
            textColor = R.color.blaze_campaign_status_rejected_text,
            backgroundColor = R.color.blaze_campaign_status_rejected_background
        ),
        Rejected(
            statusDisplayText = R.string.blaze_campaign_status_rejected,
            textColor = R.color.blaze_campaign_status_completed_text,
            backgroundColor = R.color.blaze_campaign_status_completed_background
        ),
    }

    data class LaunchBlazeCampaignCreation(val url: String, val source: BlazeFlowSource) : MultiLiveEvent.Event()
}
