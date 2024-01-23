package com.woocommerce.android.ui.blaze

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.util.TimezoneProvider
import kotlinx.coroutines.delay
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import java.util.Date
import javax.inject.Inject

class BlazeRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val blazeCampaignsStore: BlazeCampaignsStore,
    private val productDetailRepository: ProductDetailRepository,
    private val timezoneProvider: TimezoneProvider,
) {
    companion object {
        const val DEFAULT_CURRENCY_CODE = "USD" // For now only USD are supported
        const val DEFAULT_CAMPAIGN_DURATION = 7 // Days
        const val DEFAULT_CAMPAIGN_TOTAL_BUDGET = 35F // USD
        const val ONE_DAY_IN_MILLIS = 1000 * 60 * 60 * 24
    }

    suspend fun getMostRecentCampaign() = blazeCampaignsStore.getMostRecentBlazeCampaign(selectedSite.get())

    @Suppress("MagicNumber")
    fun observeLanguages() = blazeCampaignsStore.observeBlazeTargetingLanguages()

    fun getCampaignPreviewDetails(productId: Long): CampaignPreview {
        val product = productDetailRepository.getProduct(productId)
        return CampaignPreview(
            productId = productId,
            aiSuggestions = listOf(),
            budget = Budget(
                totalBudget = DEFAULT_CAMPAIGN_TOTAL_BUDGET,
                spentBudget = 0f,
                currencyCode = DEFAULT_CURRENCY_CODE,
                durationInDays = DEFAULT_CAMPAIGN_DURATION,
                startDate = Date().apply { time += ONE_DAY_IN_MILLIS }, // By default start tomorrow
            ),
            languages = listOf(),
            devices = listOf(),
            locations = listOf(),
            interests = listOf(),
            userTimeZone = timezoneProvider.deviceTimezone.displayName,
            targetUrl = product?.permalink ?: "",
            urlParams = null,
            campaignImageUrl = product?.firstImageUrl
        )
    }

    data class CampaignPreview(
        val productId: Long,
        val aiSuggestions: List<AiSuggestionForAd>,
        val budget: Budget,
        val languages: List<Language>,
        val devices: List<Device>,
        val locations: List<Location>,
        val interests: List<Interest>,
        val userTimeZone: String,
        val targetUrl: String,
        val urlParams: Pair<String, String>?,
        val campaignImageUrl: String?,
    )

    data class AiSuggestionForAd(
        val tagLine: String,
        val description: String,
    )

    data class Budget(
        val totalBudget: Float,
        val spentBudget: Float,
        val currencyCode: String,
        val durationInDays: Int,
        val startDate: Date,
    )

    data class Location(
        val id: String,
        val name: String,
    )

    data class Language(
        val code: String,
        val name: String,
    )

    data class Device(
        val id: String,
        val name: String,
    )

    data class Interest(
        val id: String,
        val description: String,
    )
}
