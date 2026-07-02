package com.woocommerce.android.ui.blaze

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.media.MediaFilesRepository
import com.woocommerce.android.media.MediaFilesRepository.ImageDetails
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.BlazeRepository.AdImageValidationResult.InvalidSize
import com.woocommerce.android.ui.blaze.BlazeRepository.AdImageValidationResult.UnsupportedMimeType
import com.woocommerce.android.ui.blaze.BlazeRepository.AdImageValidationResult.Valid
import com.woocommerce.android.ui.blaze.BlazeRepository.BlazeCampaignImage.RemoteImage
import com.woocommerce.android.ui.blaze.BlazeRepository.Budget
import com.woocommerce.android.ui.blaze.BlazeRepository.CampaignDetails
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.CAMPAIGN_BUDGET_MODE_DAILY
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.CAMPAIGN_BUDGET_MODE_TOTAL
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.CAMPAIGN_MINIMUM_DAILY_SPEND
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.DEFAULT_CAMPAIGN_DURATION
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.WEEKLY_DURATION
import com.woocommerce.android.ui.blaze.BlazeRepository.DestinationParameters
import com.woocommerce.android.ui.blaze.BlazeRepository.TargetingParameters
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignCreationRequest
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore.BlazeResult
import java.util.Date

@ExperimentalCoroutinesApi
class BlazeRepositoryTest : BaseUnitTest() {

    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn SiteModel().apply {
            url = "https://example.com"
        }
    }
    private val blazeModel: BlazeCampaignModel = BlazeCampaignModel(
        campaignId = "1",
        title = "",
        imageUrl = null,
        startTime = Date(),
        durationInDays = 0,
        uiStatus = "",
        impressions = 0,
        clicks = 0,
        targetUrn = null,
        totalBudget = 0.0,
        spentBudget = 0.0,
        isEndlessCampaign = false
    )
    private val blazeCampaignsStore: BlazeCampaignsStore = mock {
        on { createCampaign(any(), any()) } doReturn
            BlazeResult(blazeModel)
    }
    private val productDetailRepository: ProductDetailRepository = mock()
    private val mediaFilesRepository: MediaFilesRepository = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    private val createCampaignRequestCaptor = argumentCaptor<BlazeCampaignCreationRequest>()

    private val repository = BlazeRepository(
        selectedSite,
        blazeCampaignsStore,
        productDetailRepository,
        mediaFilesRepository,
        appPrefsWrapper
    )

    @Test
    fun `given an endless campaign, when creating a campaign, budget mode should be daily`() =
        testBlocking {
            repository.createCampaign(ENDLESS_CAMPAIGN_DETAILS, PAYMENT_METHOD_ID)

            verify(blazeCampaignsStore).createCampaign(any(), createCampaignRequestCaptor.capture())
            assertThat(createCampaignRequestCaptor.firstValue.budget.mode).isEqualTo(CAMPAIGN_BUDGET_MODE_DAILY)
        }

    @Test
    fun `given an endless campaign, when creating a campaign, budget should be divided by weekly duration`() =
        testBlocking {
            repository.createCampaign(ENDLESS_CAMPAIGN_DETAILS, PAYMENT_METHOD_ID)

            verify(blazeCampaignsStore).createCampaign(any(), createCampaignRequestCaptor.capture())
            assertThat(createCampaignRequestCaptor.firstValue.budget.amount)
                .isEqualTo(TOTAL_BUDGET / WEEKLY_DURATION.toDouble())
        }

    @Test
    fun `given a campaign with end date, when creating it, budget mode should be total`() =
        testBlocking {
            repository.createCampaign(NON_ENDLESS_CAMPAIGN_DETAILS, PAYMENT_METHOD_ID)

            verify(blazeCampaignsStore).createCampaign(any(), createCampaignRequestCaptor.capture())
            assertThat(createCampaignRequestCaptor.firstValue.budget.mode).isEqualTo(CAMPAIGN_BUDGET_MODE_TOTAL)
        }

    @Test
    fun `given a campaign with end date, when creating it, budget should equal totalBudget`() =
        testBlocking {
            repository.createCampaign(NON_ENDLESS_CAMPAIGN_DETAILS, PAYMENT_METHOD_ID)

            verify(blazeCampaignsStore).createCampaign(any(), createCampaignRequestCaptor.capture())
            assertThat(createCampaignRequestCaptor.firstValue.budget.amount).isEqualTo(TOTAL_BUDGET.toDouble())
        }

    @Test
    fun `given productId, when generateDefaultCampaignDetails invoked, then default CampaignDetails created`() =
        testBlocking {
            val productId = 123L
            val objective = "sales"
            val imageUrl = "https://example.com/image-400.jpg"
            val product = ProductTestUtils.generateProduct(
                productId = productId,
                productName = "My Product",
                imageUrl = imageUrl
            ).copy(
                shortDescription = "<p>Hello<br>World</p>"
            )

            whenever(appPrefsWrapper.blazeCampaignSelectedObjective).thenReturn(objective)
            whenever(productDetailRepository.getProduct(productId)).thenReturn(product)
            whenever(mediaFilesRepository.getImageDetails(imageUrl)).thenReturn(ImageDetails(0, 0, ""))

            val before = Date()
            val details = repository.generateDefaultCampaignDetails(productId)

            // Basic fields
            assertThat(details.productId).isEqualTo(productId)
            assertThat(details.tagLine).isEqualTo("My Product")
            assertThat(details.description).isEqualTo("Hello\nWorld")
            assertThat(details.ctaText).isEmpty()
            assertThat(details.objectiveId).isEqualTo(objective)
            assertThat(details.acceptedTos).isFalse()

            // Destination
            assertThat(details.destinationParameters.targetUrl).isEqualTo(product.permalink)
            assertThat(details.destinationParameters.parameters).isEmpty()

            // Targeting
            assertThat(details.targetingParameters.locations).isEmpty()
            assertThat(details.targetingParameters.languages).isEmpty()
            assertThat(details.targetingParameters.devices).isEmpty()
            assertThat(details.targetingParameters.interests).isEmpty()

            // Campaign Image
            assertThat(details.campaignImage).isInstanceOf(BlazeRepository.BlazeCampaignImage.None::class.java)

            // Budget defaults
            val expectedTotal = DEFAULT_CAMPAIGN_DURATION * CAMPAIGN_MINIMUM_DAILY_SPEND
            assertThat(details.budget.totalBudget).isEqualTo(expectedTotal)
            assertThat(details.budget.spentBudget).isEqualTo(0f)
            assertThat(details.budget.currencyCode).isEqualTo("USD")
            assertThat(details.budget.durationInDays).isEqualTo(DEFAULT_CAMPAIGN_DURATION)
            assertThat(details.budget.isEndlessCampaign).isTrue()
            // Start date should be roughly tomorrow: after 'before' and not too far in the future
            assertThat(details.budget.startDate.time).isGreaterThanOrEqualTo(before.time)
        }

    @Test
    fun `given a product with no short description, when generateDefaultCampaignDetails invoked, then CampaignDetails with description created`() =
        testBlocking {
            val productId = 456L
            val objective = "sales"
            val product = ProductTestUtils.generateProduct(
                productId = productId,
                productName = "Another Product",
                imageUrl = null
            ).copy(
                shortDescription = "",
                description = "<p>Main <br> Description</p>"
            )

            whenever(appPrefsWrapper.blazeCampaignSelectedObjective).thenReturn(objective)
            whenever(productDetailRepository.getProduct(productId)).thenReturn(product)

            val details = repository.generateDefaultCampaignDetails(productId)

            // Only focus on description defaulting behavior
            assertThat(details.description).isEqualTo("Main \n Description")
        }

    @Test
    fun `given a product with invalid image, when generateDefaultCampaignDetails invoked, then CampaignDetails with no image created`() =
        testBlocking {
            val productId = 456L
            val objective = "sales"
            val imageUrl = "https://example.com/image-400.jpg"
            val product = ProductTestUtils.generateProduct(
                productId = productId,
                productName = "Another Product",
                imageUrl = imageUrl
            )

            whenever(appPrefsWrapper.blazeCampaignSelectedObjective).thenReturn(objective)
            whenever(productDetailRepository.getProduct(productId)).thenReturn(product)
            whenever(mediaFilesRepository.getImageDetails(imageUrl)).thenReturn(ImageDetails(0, 0, ""))

            val details = repository.generateDefaultCampaignDetails(productId)

            // Only focus on campaign image behavior
            assertThat(details.campaignImage).isInstanceOf(BlazeRepository.BlazeCampaignImage.None::class.java)
        }

    @Test
    fun `given supported image mime types, when validating ad image, then result is valid`() {
        SUPPORTED_IMAGE_MIME_TYPES.forEach { mimeType ->
            // GIVEN
            val imageDetails = ImageDetails(width = VALID_IMAGE_SIZE, height = VALID_IMAGE_SIZE, mimeType = mimeType)

            // WHEN
            val validationResult = with(repository) { imageDetails.validateAdImage() }

            // THEN
            assertThat(validationResult).isEqualTo(Valid)
        }
    }

    @Test
    fun `given supported image mime type with extra whitespace, when validating ad image, then result is valid`() {
        // GIVEN
        val imageDetails = ImageDetails(width = VALID_IMAGE_SIZE, height = VALID_IMAGE_SIZE, mimeType = " image/jpeg ")

        // WHEN
        val validationResult = with(repository) { imageDetails.validateAdImage() }

        // THEN
        assertThat(validationResult).isEqualTo(Valid)
    }

    @Test
    fun `given unsupported image mime types, when validating ad image, then result is unsupported mime type`() {
        UNSUPPORTED_IMAGE_MIME_TYPES.forEach { mimeType ->
            // GIVEN
            val imageDetails = ImageDetails(width = VALID_IMAGE_SIZE, height = VALID_IMAGE_SIZE, mimeType = mimeType)

            // WHEN
            val validationResult = with(repository) { imageDetails.validateAdImage() }

            // THEN
            assertThat(validationResult).isEqualTo(UnsupportedMimeType)
        }
    }

    @Test
    fun `given unsupported image mime type with small dimensions, when validating ad image, then result is unsupported mime type`() {
        // GIVEN
        val imageDetails = ImageDetails(
            width = VALID_IMAGE_SIZE - 1,
            height = VALID_IMAGE_SIZE,
            mimeType = "image/avif"
        )

        // WHEN
        val validationResult = with(repository) { imageDetails.validateAdImage() }

        // THEN
        assertThat(validationResult).isEqualTo(UnsupportedMimeType)
    }

    @Test
    fun `given supported image mime type with small dimensions, when validating ad image, then result is invalid size`() {
        // GIVEN
        val imageDetails = ImageDetails(
            width = VALID_IMAGE_SIZE - 1,
            height = VALID_IMAGE_SIZE,
            mimeType = "image/jpeg"
        )

        // WHEN
        val validationResult = with(repository) { imageDetails.validateAdImage() }

        // THEN
        assertThat(validationResult).isEqualTo(InvalidSize)
    }

    companion object {
        private const val TOTAL_BUDGET = 35f
        private const val PAYMENT_METHOD_ID = "132435"
        private const val VALID_IMAGE_SIZE = 400
        private val SUPPORTED_IMAGE_MIME_TYPES = listOf(
            "image/png",
            "image/x-png",
            "image/webp",
            "image/gif",
            "image/jpeg",
            "image/bmp",
            "image/heic",
            "image/heif"
        )
        private val UNSUPPORTED_IMAGE_MIME_TYPES = listOf(
            "image/avif",
            "image/svg+xml",
            "application/pdf"
        )
        private val AD_IMAGE = RemoteImage(
            uri = "https://example.com/image.jpg",
            mimeType = "image/jpeg",
        )
        private val DEFAULT_START_DATE = Date()
        private val EMPTY_TARGETING_PARAMETERS = TargetingParameters(
            locations = emptyList(),
            languages = emptyList(),
            devices = emptyList(),
            interests = emptyList()
        )
        private val EMPTY_DESTINATION_PARAMETERS = DestinationParameters(
            targetUrl = "",
            parameters = emptyMap(),
        )
        private val DEFAULT_BUDGET = Budget(
            totalBudget = TOTAL_BUDGET,
            spentBudget = 5F,
            currencyCode = "USD",
            durationInDays = 7,
            startDate = DEFAULT_START_DATE,
            isEndlessCampaign = true,
        )
        private val DEFAULT_CAMPAIGN_DETAILS =
            CampaignDetails(
                productId = 1,
                tagLine = "",
                description = "",
                ctaText = "",
                campaignImage = AD_IMAGE,
                budget = DEFAULT_BUDGET,
                targetingParameters = EMPTY_TARGETING_PARAMETERS,
                destinationParameters = EMPTY_DESTINATION_PARAMETERS,
                objectiveId = "sales",
                acceptedTos = false,
            )
        private val ENDLESS_CAMPAIGN_DETAILS = DEFAULT_CAMPAIGN_DETAILS.copy(
            budget = DEFAULT_BUDGET.copy(isEndlessCampaign = true)
        )
        private val NON_ENDLESS_CAMPAIGN_DETAILS = DEFAULT_CAMPAIGN_DETAILS.copy(
            budget = DEFAULT_BUDGET.copy(isEndlessCampaign = false)
        )
    }
}
