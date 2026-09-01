package com.woocommerce.android.ui.blaze

import android.os.Parcelable
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.AppUrls.FETCH_PAYMENT_METHOD_URL_PATH
import com.woocommerce.android.AppUrls.WPCOM_ADD_PAYMENT_METHOD
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.media.MediaFilesRepository
import com.woocommerce.android.model.CreditCardType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.joinToUrl
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.transform
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.blaze.BlazeAdForecast
import org.wordpress.android.fluxc.model.blaze.BlazeAdSuggestion
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignCreationRequest
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignCreationRequestBudget
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignCreationRequestImage
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignType
import org.wordpress.android.fluxc.model.blaze.BlazePaymentMethod.PaymentMethodInfo
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingParameters
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.days

class BlazeRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val blazeCampaignsStore: BlazeCampaignsStore,
    private val productDetailRepository: ProductDetailRepository,
    private val mediaFilesRepository: MediaFilesRepository,
    private val appPrefsWrapper: AppPrefsWrapper
) {
    companion object {
        private const val BLAZE_CAMPAIGN_CREATION_ORIGIN = "wc-android"
        const val CAMPAIGN_BUDGET_MODE_TOTAL = "total" // "total" for campaigns with defined end date
        const val CAMPAIGN_BUDGET_MODE_DAILY = "daily" // "daily" for endless/evergreen campaigns
        const val BLAZE_DEFAULT_CURRENCY_CODE = "USD" // For now only USD are supported
        const val DEFAULT_CAMPAIGN_DURATION = 7 // Days
        const val CAMPAIGN_MINIMUM_DAILY_SPEND = 5f // USD
        const val CAMPAIGN_MAXIMUM_DAILY_SPEND = 50f // USD
        const val CAMPAIGN_MAX_DURATION = 28 // Days
        const val BLAZE_IMAGE_MINIMUM_SIZE_IN_PIXELS = 400 // Must be at least 400 x 400 pixels
        const val WEEKLY_DURATION = 7 // Used to calculate weekly budget in endless campaigns
        private val SUPPORTED_BLAZE_IMAGE_MIME_TYPES = setOf(
            "image/png",
            "image/x-png",
            "image/webp",
            "image/gif",
            "image/jpeg",
            "image/bmp",
            "image/heic",
            "image/heif"
        )
    }

    fun observeObjectives() = blazeCampaignsStore.observeBlazeCampaignObjectives().map {
        it.map { objective ->
            Objective(
                objective.id,
                objective.title,
                objective.description,
                objective.suitableForDescription
            )
        }
    }

    suspend fun fetchObjectives(): Result<Unit> {
        val result = blazeCampaignsStore.fetchBlazeCampaignObjectives(selectedSite.get())

        return when {
            result.isError -> {
                WooLog.w(WooLog.T.BLAZE, "Failed to fetch objectives: ${result.error}")
                Result.failure(OnChangedException(result.error))
            }

            else -> Result.success(Unit)
        }
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
            return map { AiSuggestionForAd(it.tagLine, it.description, it.ctaText) }
        }

        val result = blazeCampaignsStore.fetchBlazeAdSuggestions(selectedSite.get(), productId)

        return when {
            result.isError -> {
                WooLog.w(WooLog.T.BLAZE, "Failed to fetch ad suggestions: ${result.error}")
                Result.failure(OnChangedException(result.error, result.error.message))
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
            isEndlessCampaign = true
        )

        val product = productDetailRepository.getProduct(productId)
            ?: productDetailRepository.fetchAndGetProduct(productId)!!

        val description = product.shortDescription.takeIf { product.hasShortDescription } ?: product.description
        return CampaignDetails(
            productId = productId,
            tagLine = product.name,
            description = description.fastStripHtml(),
            campaignImage = product.images.firstOrNull()?.let { image ->
                when (val validationResult = getImageDetails(image.source).validateAdImage()) {
                    is AdImageValidationResult.Valid ->
                        BlazeCampaignImage.RemoteImage(image.source, validationResult.mimeType)

                    AdImageValidationResult.InvalidSize,
                    AdImageValidationResult.UnsupportedMimeType -> BlazeCampaignImage.None
                }
            } ?: BlazeCampaignImage.None,
            budget = getDefaultBudget(),
            targetingParameters = TargetingParameters(),
            destinationParameters = DestinationParameters(
                targetUrl = product.permalink,
                parameters = emptyMap()
            ),
            objectiveId = appPrefsWrapper.blazeCampaignSelectedObjective,
            ctaText = "",
            acceptedTos = false
        )
    }

    suspend fun fetchAdForecast(
        startDate: Date,
        campaignDurationDays: Int,
        totalBudget: Float,
        targetingParameters: TargetingParameters
    ): Result<BlazeAdForecast> {
        val result = blazeCampaignsStore.fetchBlazeAdForecast(
            siteModel = selectedSite.get(),
            startDate = startDate,
            endDate = Date(startDate.time + campaignDurationDays.days.inWholeMilliseconds),
            totalBudget = totalBudget.roundToInt().toDouble(),
            targetingParameters = targetingParameters.let {
                BlazeTargetingParameters(
                    locations = it.locations.map { location -> location.id },
                    languages = it.languages.map { language -> language.code },
                    devices = it.devices.map { device -> device.id },
                    topics = it.interests.map { interest -> interest.id }
                )
            }
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
                        addPaymentMethodUrls = createPaymentMethodUrls()
                    )
                )
            } ?: Result.failure(NullPointerException("API response is null"))
        }
    }

    private fun createPaymentMethodUrls(): PaymentMethodUrls {
        return PaymentMethodUrls(
            formUrl = WPCOM_ADD_PAYMENT_METHOD,
            successUrl = FETCH_PAYMENT_METHOD_URL_PATH
        )
    }

    @Suppress("LongMethod")
    suspend fun createCampaign(
        campaignDetails: CampaignDetails,
        paymentMethodId: String
    ): Result<Unit> {
        val image = prepareCampaignImage(campaignDetails.campaignImage).getOrElse {
            return Result.failure(
                when (it) {
                    is MediaFilesRepository.MediaUploadException -> CampaignCreationError.MediaUploadError(it.message)
                    is OnChangedException -> CampaignCreationError.MediaFetchError(it.message)
                    else -> it
                }
            )
        }

        val result = blazeCampaignsStore.createCampaign(
            selectedSite.get(),
            request = BlazeCampaignCreationRequest(
                origin = BLAZE_CAMPAIGN_CREATION_ORIGIN,
                originVersion = BuildConfig.VERSION_NAME,
                type = BlazeCampaignType.PRODUCT,
                paymentMethodId = paymentMethodId,
                targetResourceId = campaignDetails.productId,
                tagLine = campaignDetails.tagLine,
                description = campaignDetails.description,
                ctaText = campaignDetails.ctaText,
                startDate = campaignDetails.budget.startDate,
                endDate = campaignDetails.budget.endDate,
                budget = BlazeCampaignCreationRequestBudget(
                    mode = when {
                        campaignDetails.budget.isEndlessCampaign -> CAMPAIGN_BUDGET_MODE_DAILY
                        else -> CAMPAIGN_BUDGET_MODE_TOTAL
                    },
                    amount = when {
                        campaignDetails.budget.isEndlessCampaign -> campaignDetails.budget.totalBudget / WEEKLY_DURATION
                        else -> campaignDetails.budget.totalBudget
                    }.toDouble(),
                    currency = BLAZE_DEFAULT_CURRENCY_CODE // To be replaced when more currencies are supported
                ),
                targetUrl = campaignDetails.destinationParameters.targetUrl,
                urlParams = campaignDetails.destinationParameters.parameters,
                mainImage = image,
                targetingParameters = campaignDetails.targetingParameters.let {
                    BlazeTargetingParameters(
                        locations = it.locations.map { location -> location.id },
                        languages = it.languages.map { language -> language.code },
                        devices = it.devices.map { device -> device.id },
                        topics = it.interests.map { interest -> interest.id }
                    )
                },
                isEndlessCampaign = campaignDetails.budget.isEndlessCampaign,
                objectiveId = campaignDetails.objectiveId,
                acceptedTos = campaignDetails.acceptedTos,
            )
        )

        return when {
            result.isError -> {
                WooLog.w(WooLog.T.BLAZE, "Failed to create campaign: ${result.error}")
                Result.failure(CampaignCreationError.CampaignApiError(result.error.message))
            }

            else -> {
                WooLog.d(WooLog.T.BLAZE, "Campaign created successfully")
                Result.success(Unit)
            }
        }
    }

    private suspend fun prepareCampaignImage(image: BlazeCampaignImage): Result<BlazeCampaignCreationRequestImage> {
        val result = when (image) {
            is BlazeCampaignImage.LocalImage -> {
                mediaFilesRepository.uploadFile(image.uri)
                    .transform {
                        when (it) {
                            is MediaFilesRepository.UploadResult.UploadSuccess -> {
                                val requestImage = BlazeCampaignCreationRequestImage(
                                    url = it.media.url,
                                    mimeType = it.media.mimeType.orEmpty()
                                )
                                emit(Result.success(requestImage))
                            }

                            is MediaFilesRepository.UploadResult.UploadFailure -> throw it.error
                            else -> {
                                /* Do nothing */
                            }
                        }
                    }
                    .retry(1)
                    .catch { emit(Result.failure(it)) }
                    .first()
            }

            is BlazeCampaignImage.RemoteImage -> Result.success(
                BlazeCampaignCreationRequestImage(
                    url = image.uri,
                    mimeType = image.mimeType
                )
            )

            is BlazeCampaignImage.None -> error("No image provided for Blaze Campaign Creation")
        }

        return result.onFailure {
            WooLog.w(WooLog.T.BLAZE, "Failed to prepare campaign image: ${it.message}")
        }
    }

    suspend fun getImageDetails(uri: String) = mediaFilesRepository.getImageDetails(uri)

    fun MediaFilesRepository.ImageDetails.validateAdImage(): AdImageValidationResult {
        val normalizedMimeType = mimeType?.trim()?.lowercase(Locale.US)
        return when {
            normalizedMimeType == null ||
                normalizedMimeType !in SUPPORTED_BLAZE_IMAGE_MIME_TYPES -> AdImageValidationResult.UnsupportedMimeType

            width < BLAZE_IMAGE_MINIMUM_SIZE_IN_PIXELS ||
                height < BLAZE_IMAGE_MINIMUM_SIZE_IN_PIXELS -> AdImageValidationResult.InvalidSize

            else -> AdImageValidationResult.Valid(normalizedMimeType)
        }
    }

    fun isCampaignObjectiveSwitchChecked() = appPrefsWrapper.blazeCampaignObjectiveSwitchChecked

    fun setCampaignObjectiveSwitchChecked(enabled: Boolean) {
        appPrefsWrapper.blazeCampaignObjectiveSwitchChecked = enabled
    }

    fun storeSelectedObjective(objectiveId: String) {
        appPrefsWrapper.blazeCampaignSelectedObjective = objectiveId
    }

    sealed interface AdImageValidationResult {
        data class Valid(val mimeType: String) : AdImageValidationResult
        data object InvalidSize : AdImageValidationResult
        data object UnsupportedMimeType : AdImageValidationResult
    }

    @Parcelize
    data class CampaignDetails(
        val productId: Long,
        val tagLine: String,
        val description: String,
        val ctaText: String,
        val campaignImage: BlazeCampaignImage,
        val budget: Budget,
        val targetingParameters: TargetingParameters,
        val destinationParameters: DestinationParameters,
        val objectiveId: String,
        val acceptedTos: Boolean
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
        data class RemoteImage(override val uri: String, val mimeType: String) : BlazeCampaignImage
    }

    @Parcelize
    data class Objective(
        val id: String,
        val title: String,
        val description: String,
        val suitableForDescription: String
    ) : Parcelable

    @Parcelize
    data class TargetingParameters(
        val locations: List<Location> = emptyList(),
        val languages: List<Language> = emptyList(),
        val devices: List<Device> = emptyList(),
        val interests: List<Interest> = emptyList()
    ) : Parcelable

    @Parcelize
    data class DestinationParameters(
        val targetUrl: String,
        val parameters: Map<String, String>
    ) : Parcelable {
        val fullUrl: String
            get() = parameters.joinToUrl(targetUrl)
    }

    @Parcelize
    data class AiSuggestionForAd(
        val tagLine: String,
        val description: String,
        val ctaText: String
    ) : Parcelable

    @Parcelize
    data class Budget(
        val totalBudget: Float,
        val spentBudget: Float,
        val currencyCode: String,
        val durationInDays: Int,
        val startDate: Date,
        val isEndlessCampaign: Boolean
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
        val successUrl: String
    ) : Parcelable

    sealed class CampaignCreationError(message: String?) : Exception(message) {
        class MediaUploadError(message: String?) : CampaignCreationError(message)
        class MediaFetchError(message: String?) : CampaignCreationError(message)
        class CampaignApiError(message: String?) : CampaignCreationError(message)
    }
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
