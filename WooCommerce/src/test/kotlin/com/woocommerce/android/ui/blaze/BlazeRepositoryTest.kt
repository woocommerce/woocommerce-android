package com.woocommerce.android.ui.blaze

import com.woocommerce.android.media.MediaFilesRepository
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.BlazeRepository.BlazeCampaignImage.RemoteImage
import com.woocommerce.android.ui.blaze.BlazeRepository.Budget
import com.woocommerce.android.ui.blaze.BlazeRepository.CampaignDetails
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.CAMPAIGN_BUDGET_MODE_DAILY
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.CAMPAIGN_BUDGET_MODE_TOTAL
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.WEEKLY_DURATION
import com.woocommerce.android.ui.blaze.BlazeRepository.DestinationParameters
import com.woocommerce.android.ui.blaze.BlazeRepository.TargetingParameters
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
import org.wordpress.android.fluxc.model.MediaModel
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
    private val blazeModel: BlazeCampaignModel = mock()
    private val blazeCampaignsStore: BlazeCampaignsStore = mock {
        onBlocking { createCampaign(any(), any()) } doReturn
            BlazeResult(blazeModel)
    }
    private val productDetailRepository: ProductDetailRepository = mock()
    private val mediaModel: MediaModel = mock()
    private val mediaFilesRepository: MediaFilesRepository = mock {
        onBlocking { fetchWordPressMedia(AD_IMAGE.mediaId) } doReturn
            Result.success(mediaModel)
    }

    private val createCampaignRequestCaptor = argumentCaptor<BlazeCampaignCreationRequest>()

    private val repository = BlazeRepository(
        selectedSite,
        blazeCampaignsStore,
        productDetailRepository,
        mediaFilesRepository
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

    companion object {
        private const val TOTAL_BUDGET = 35f
        private const val PAYMENT_METHOD_ID = "132435"
        private val AD_IMAGE = RemoteImage(
            mediaId = 1,
            uri = "https://example.com/image.jpg",
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
                campaignImage = AD_IMAGE,
                budget = DEFAULT_BUDGET,
                targetingParameters = EMPTY_TARGETING_PARAMETERS,
                destinationParameters = EMPTY_DESTINATION_PARAMETERS,
            )
        private val ENDLESS_CAMPAIGN_DETAILS = DEFAULT_CAMPAIGN_DETAILS.copy(
            budget = DEFAULT_BUDGET.copy(isEndlessCampaign = true)
        )
        private val NON_ENDLESS_CAMPAIGN_DETAILS = DEFAULT_CAMPAIGN_DETAILS.copy(
            budget = DEFAULT_BUDGET.copy(isEndlessCampaign = false)
        )
    }
}
