package com.woocommerce.android.ui.blaze

import android.os.Parcelable
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.model.CreditCardType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.util.TimezoneProvider
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.blaze.BlazeAdForecast
import org.wordpress.android.fluxc.model.blaze.BlazeAdSuggestion
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignCreationRequest
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignType
import org.wordpress.android.fluxc.model.blaze.BlazePaymentMethod.PaymentMethodInfo
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingParameters
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import java.util.Date
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.days

class BlazeRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val blazeCampaignsStore: BlazeCampaignsStore,
    private val productDetailRepository: ProductDetailRepository,
    private val timezoneProvider: TimezoneProvider,
) {
    companion object {
        const val BLAZE_DEFAULT_CURRENCY_CODE = "USD" // For now only USD are supported
        const val DEFAULT_CAMPAIGN_DURATION = 7 // Days
        const val CAMPAIGN_MINIMUM_DAILY_SPEND = 5f // USD
        const val CAMPAIGN_MAXIMUM_DAILY_SPEND = 50f // USD
        const val CAMPAIGN_MAX_DURATION = 28 // Days
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

    suspend fun generateDefaultCampaignDetails(productId: Long): CampaignDetails {
        fun getDefaultBudget() = Budget(
            totalBudget = DEFAULT_CAMPAIGN_DURATION * CAMPAIGN_MINIMUM_DAILY_SPEND,
            spentBudget = 0f,
            currencyCode = BLAZE_DEFAULT_CURRENCY_CODE,
            durationInDays = DEFAULT_CAMPAIGN_DURATION,
            startDate = Date().apply { time += 1.days.inWholeMilliseconds }, // By default start tomorrow
        )

        val product = productDetailRepository.getProduct(productId)
            ?: productDetailRepository.fetchProductOrLoadFromCache(productId)!!

        return CampaignDetails(
            productId = productId,
            userTimeZone = timezoneProvider.deviceTimezone.displayName,
            tagLine = "",
            description = "",
            targetUrl = product.permalink,
            urlParams = emptyMap(),
            campaignImage = product.images.firstOrNull().let {
                if (it != null) BlazeCampaignImage.RemoteImage(it.id, it.source)
                else BlazeCampaignImage.None
            },
            budget = getDefaultBudget(),
            targetingParameters = TargetingParameters()
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
            Date(startDate.time + campaignDurationDays.days.inWholeMilliseconds),
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

    suspend fun fetchPaymentMethods(): Result<PaymentMethodsData> {
        val result = blazeCampaignsStore.fetchBlazePaymentMethods(selectedSite.get())

        return when {
            result.isError -> {
                WooLog.w(WooLog.T.BLAZE, "Failed to fetch payment methods: ${result.error}")
                Result.failure(OnChangedException(result.error))
            }

            else -> result.model?.let { paymentMethods ->
                Result.success(
                    PaymentMethodsData(
                        savedPaymentMethods = paymentMethods.savedPaymentMethods.map { paymentMethod ->
                            PaymentMethod(
                                id = paymentMethod.id,
                                name = paymentMethod.name,
                                info = when (paymentMethod.info) {
                                    is PaymentMethodInfo.CreditCardInfo ->
                                        (paymentMethod.info as PaymentMethodInfo.CreditCardInfo).let {
                                            PaymentMethod.PaymentMethodInfo.CreditCard(
                                                creditCardType = CreditCardType.fromString(it.type),
                                                cardHolderName = it.cardHolderName
                                            )
                                        }

                                    PaymentMethodInfo.Unknown -> {
                                        PaymentMethod.PaymentMethodInfo.Unknown
                                    }
                                }
                            )
                        },
                        addPaymentMethodUrls = PaymentMethodUrls(
                            formUrl = paymentMethods.addPaymentMethodUrls.formUrl,
                            successUrl = paymentMethods.addPaymentMethodUrls.successUrl,
                            idUrlParameter = paymentMethods.addPaymentMethodUrls.idUrlParameter
                        )
                    )
                )
            } ?: Result.failure(NullPointerException("API response is null"))
        }
    }

    suspend fun createCampaign(
        campaignDetails: CampaignDetails,
        paymentMethodId: String
    ): Result<Unit> {
        val image = prepareCampaignImage().getOrElse {
            return Result.failure(it)
        }

        val result = blazeCampaignsStore.createCampaign(
            selectedSite.get(),
            request = BlazeCampaignCreationRequest(
                origin = "wc-android",
                originVersion = BuildConfig.VERSION_NAME,
                type = BlazeCampaignType.PRODUCT,
                paymentMethodId = paymentMethodId,
                targetResourceId = campaignDetails.productId,
                tagLine = campaignDetails.tagLine,
                description = campaignDetails.description,
                startDate = campaignDetails.budget.startDate,
                endDate = campaignDetails.budget.endDate,
                budget = campaignDetails.budget.totalBudget.toDouble(),
                targetUrl = campaignDetails.targetUrl,
                urlParams = campaignDetails.urlParams,
                mainImage = image,
                targetingParameters = campaignDetails.targetingParameters.let {
                    BlazeTargetingParameters(
                        locations = it.locations.map { location -> location.id.toString() },
                        languages = it.languages.map { language -> language.code },
                        devices = it.devices.map { device -> device.id },
                        topics = it.interests.map { interest -> interest.id }
                    )
                }
            )
        )

        return when {
            result.isError -> {
                WooLog.w(WooLog.T.BLAZE, "Failed to create campaign: ${result.error}")
                Result.failure(OnChangedException(result.error))
            }

            else -> Result.success(Unit)
        }
    }

    private fun prepareCampaignImage(): Result<MediaModel> {
        TODO()
    }

    @Parcelize
    data class CampaignDetails(
        val productId: Long,
        val tagLine: String,
        val description: String,
        val userTimeZone: String,
        val targetUrl: String,
        val urlParams: Map<String, String>,
        val campaignImage: BlazeCampaignImage,
        val budget: Budget,
        val targetingParameters: TargetingParameters
    ) : Parcelable

    sealed interface BlazeCampaignImage : Parcelable {
        val uri: String
        @Parcelize
        data object None : BlazeCampaignImage {
            override val uri: String
                get() = ""
        }

        @Parcelize
        data class LocalImage(override val uri: String) : BlazeCampaignImage

        @Parcelize
        data class RemoteImage(val mediaId: Long, override val uri: String) : BlazeCampaignImage
    }

    @Parcelize
    data class TargetingParameters(
        val locations: List<Location> = emptyList(),
        val languages: List<Language> = emptyList(),
        val devices: List<Device> = emptyList(),
        val interests: List<Interest> = emptyList()
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
    ) : Parcelable {
        val endDate: Date
            get() = Date(startDate.time + durationInDays.days.inWholeMilliseconds)
    }

    @Parcelize
    data class PaymentMethodsData(
        val savedPaymentMethods: List<PaymentMethod>,
        val addPaymentMethodUrls: PaymentMethodUrls
    ) : Parcelable

    @Parcelize
    data class PaymentMethod(
        val id: String,
        val name: String,
        val info: PaymentMethodInfo
    ) : Parcelable {
        sealed interface PaymentMethodInfo : Parcelable {
            @Parcelize
            data class CreditCard(
                val creditCardType: CreditCardType,
                val cardHolderName: String
            ) : PaymentMethodInfo

            @Parcelize
            data object Unknown : PaymentMethodInfo
        }
    }

    @Parcelize
    data class PaymentMethodUrls(
        val formUrl: String,
        val successUrl: String,
        val idUrlParameter: String
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
