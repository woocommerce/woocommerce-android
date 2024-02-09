package com.woocommerce.android.ui.blaze

import android.os.Parcelable
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.util.TimezoneProvider
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.blaze.BlazeAdForecast
import org.wordpress.android.fluxc.model.blaze.BlazeAdSuggestion
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import java.util.Date
import javax.inject.Inject
import kotlin.math.roundToInt

class BlazeRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val blazeCampaignsStore: BlazeCampaignsStore,
    private val productDetailRepository: ProductDetailRepository,
    private val timezoneProvider: TimezoneProvider,
) {
    companion object {
        const val BLAZE_DEFAULT_CURRENCY_CODE = "USD" // For now only USD are supported
        const val DEFAULT_CAMPAIGN_DURATION = 7 // Days
        const val CAMPAIGN_MINIMUM_DAILY_SPEND = 5F // USD
        const val CAMPAIGN_MAXIMUM_DAILY_SPEND = 50F // USD
        const val CAMPAIGN_MAX_DURATION = 28 // Days
        const val ONE_DAY_IN_MILLIS: Long = 1000 * 60 * 60 * 24
    }

    fun observeLanguages() = blazeCampaignsStore.observeBlazeTargetingLanguages()
        .map { it.map { language -> Language(language.id, language.name) } }

    suspend fun fetchLanguages(): Result<Unit> {
        val result = blazeCampaignsStore.fetchBlazeTargetingLanguages(selectedSite.get())

        return when {
            result.isError -> {
                WooLog.w(WooLog.T.BLAZE, "Failed to fetch languages: ${result.error}")
                Result.failure(OnChangedException(result.error))
            }

            else -> Result.success(Unit)
        }
    }

    fun observeDevices() = blazeCampaignsStore.observeBlazeTargetingDevices()
        .map { it.map { device -> Device(device.id, device.name) } }

    suspend fun fetchDevices(): Result<Unit> {
        val result = blazeCampaignsStore.fetchBlazeTargetingDevices(selectedSite.get())

        return when {
            result.isError -> {
                WooLog.w(WooLog.T.BLAZE, "Failed to fetch devices: ${result.error}")
                Result.failure(OnChangedException(result.error))
            }

            else -> Result.success(Unit)
        }
    }

    fun observeInterests() = blazeCampaignsStore.observeBlazeTargetingTopics()
        .map { it.map { interest -> Interest(interest.id, interest.description) } }

    suspend fun fetchInterests(): Result<Unit> {
        val result = blazeCampaignsStore.fetchBlazeTargetingTopics(selectedSite.get())

        return when {
            result.isError -> {
                WooLog.w(WooLog.T.BLAZE, "Failed to fetch interests: ${result.error}")
                Result.failure(OnChangedException(result.error))
            }

            else -> Result.success(Unit)
        }
    }

    suspend fun fetchLocations(query: String): Result<List<Location>> {
        val result = blazeCampaignsStore.fetchBlazeTargetingLocations(
            selectedSite.get(),
            query
        )

        return when {
            result.isError -> {
                WooLog.w(WooLog.T.BLAZE, "Failed to fetch locations: ${result.error}")
                Result.failure(OnChangedException(result.error))
            }

            else -> Result.success(
                result.model?.map { location ->
                    Location(location.id, location.name, location.parent?.name, location.type)
                } ?: emptyList()
            )
        }
    }

    suspend fun getMostRecentCampaign() = blazeCampaignsStore.getMostRecentBlazeCampaign(selectedSite.get())

    suspend fun fetchAdSuggestions(productId: Long): Result<List<AiSuggestionForAd>> {
        fun List<BlazeAdSuggestion>.mapToUiModel(): List<AiSuggestionForAd> {
            return map { AiSuggestionForAd(it.tagLine, it.description) }
        }

        val result = blazeCampaignsStore.fetchBlazeAdSuggestions(selectedSite.get(), productId)

        return when {
            result.isError -> {
                WooLog.w(WooLog.T.BLAZE, "Failed to fetch ad suggestions: ${result.error}")
                Result.failure(OnChangedException(result.error))
            }

            else -> Result.success(result.model?.mapToUiModel() ?: emptyList())
        }
    }

    fun getCampaignPreviewDetails(productId: Long): CampaignPreview {
        val product = productDetailRepository.getProduct(productId)
        return CampaignPreview(
            productId = productId,
            userTimeZone = timezoneProvider.deviceTimezone.displayName,
            targetUrl = product?.permalink ?: "",
            urlParams = null,
            campaignImageUrl = product?.firstImageUrl
        )
    }

    suspend fun fetchAdForecast(
        startDate: Date,
        campaignDurationDays: Int,
        totalBudget: Float
    ): Result<BlazeAdForecast> {
        val result = blazeCampaignsStore.fetchBlazeAdForecast(
            selectedSite.get(),
            startDate,
            Date(startDate.time + campaignDurationDays * ONE_DAY_IN_MILLIS),
            totalBudget.roundToInt().toDouble(),
        )
        return when {
            result.isError -> {
                WooLog.w(WooLog.T.BLAZE, "Failed to fetch ad forecast: ${result.error}")
                Result.failure(OnChangedException(result.error))
            }

            else -> Result.success(result.model!!)
        }
    }

    @Parcelize
    data class CampaignPreview(
        val productId: Long,
        val userTimeZone: String,
        val targetUrl: String,
        val urlParams: Pair<String, String>?,
        val campaignImageUrl: String?,
    ) : Parcelable

    @Parcelize
    data class AiSuggestionForAd(
        val tagLine: String,
        val description: String,
    ) : Parcelable

    @Parcelize
    data class Budget(
        val totalBudget: Float,
        val spentBudget: Float,
        val currencyCode: String,
        val durationInDays: Int,
        val startDate: Date,
    ) : Parcelable
}

@Parcelize
data class Location(
    val id: Long,
    val name: String,
    val parent: String? = null,
    val type: String? = null
) : Parcelable

@Parcelize
data class Language(
    val code: String,
    val name: String,
) : Parcelable

@Parcelize
data class Device(
    val id: String,
    val name: String,
) : Parcelable

@Parcelize
data class Interest(
    val id: String,
    val description: String,
) : Parcelable
