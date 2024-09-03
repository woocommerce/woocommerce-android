package com.woocommerce.android.ui.blaze.campaigs

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignsModel
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsError
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsUtils
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore.BlazeCampaignsResult

@ExperimentalCoroutinesApi
class BlazeCampaignListViewModelTest : BaseUnitTest() {
    private val blazeCampaignsStore: BlazeCampaignsStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val blazeUrlsHelper: BlazeUrlsHelper = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val siteModel: SiteModel = mock()
    private val campaignsEntityFlow = flow { emit(listOf(BLAZE_CAMPAIGN_MODEL)) }
    private val currencyFormatter: CurrencyFormatter = mock()

    private lateinit var viewModel: BlazeCampaignListViewModel

    @Before
    fun setup() = testBlocking {
        whenever(selectedSite.get()).thenReturn(siteModel)
        whenever(currencyFormatter.formatCurrencyRounded(TOTAL_BUDGET)).thenReturn(TOTAL_BUDGET.toString())
        whenever(blazeCampaignsStore.observeBlazeCampaigns(selectedSite.get())).thenReturn(campaignsEntityFlow)
        whenever(blazeCampaignsStore.fetchBlazeCampaigns(any(), any(), any(), any(), eq(null)))
            .thenReturn(BlazeCampaignsResult(EMPTY_BLAZE_CAMPAIGN_MODEL))
    }

    @Test
    fun `when screen loaded, subscribe to blaze campaigns changes`() = testBlocking {
        createViewModel()

        verify(blazeCampaignsStore).observeBlazeCampaigns(siteModel)
    }

    @Test
    fun `when screen loaded, fetch campaigns for first batch`() = testBlocking {
        whenever(blazeCampaignsStore.fetchBlazeCampaigns(any(), any(), any(), any(), eq(null)))
            .thenReturn(BlazeCampaignsResult(EMPTY_BLAZE_CAMPAIGN_MODEL))

        createViewModel()

        verify(blazeCampaignsStore).fetchBlazeCampaigns(
            siteModel,
            offset = 0
        )
    }

    @Test
    fun `given one item was fetched, when loading more, then offset the first item and load next`() = testBlocking {
        whenever(blazeCampaignsStore.fetchBlazeCampaigns(any(), any(), any(), any(), eq(null)))
            .thenReturn(BlazeCampaignsResult(BLAZE_CAMPAIGN_MODEL_1_OUT_OF_2_ITEMS))
        createViewModel()
        viewModel.state.captureValues().last()

        viewModel.onLoadMoreCampaigns()

        verify(blazeCampaignsStore).fetchBlazeCampaigns(
            siteModel,
            offset = 1
        )
    }

    @Test
    fun `given fetching campaigns fails, when reaching end of the list, then show error snackbar`() = testBlocking {
        whenever(blazeCampaignsStore.fetchBlazeCampaigns(any(), any(), any(), any(), eq(null)))
            .thenReturn(BlazeCampaignsResult(BlazeCampaignsError(INVALID_RESPONSE)))
        createViewModel()

        viewModel.onLoadMoreCampaigns()
        val errorEvent = viewModel.event.captureValues().filterIsInstance<Event.ShowSnackbar>().last()

        Assertions.assertThat(errorEvent.message).isEqualTo(R.string.blaze_campaign_list_error_fetching_campaigns)
    }

    @Test
    fun `given celebration not shown before, when opening post campaign creation, show celebration`() = testBlocking {
        whenever(appPrefsWrapper.isBlazeCelebrationScreenShown).thenReturn(false)

        createViewModel(isPostCampaignCreation = true)
        val state = viewModel.state.captureValues().last()

        Assertions.assertThat(state.isCampaignCelebrationShown).isTrue()
        verify(appPrefsWrapper).isBlazeCelebrationScreenShown = true
    }

    @Test
    fun `given celebration shown before, when opening post campaign creation, do not show celebration`() =
        testBlocking {
            whenever(appPrefsWrapper.isBlazeCelebrationScreenShown).thenReturn(true)

            createViewModel(isPostCampaignCreation = true)
            val state = viewModel.state.captureValues().last()

            Assertions.assertThat(state.isCampaignCelebrationShown).isFalse()
        }

    @Test
    fun `given celebration shown, when dismissing celebration, hide it`() = testBlocking {
        whenever(appPrefsWrapper.isBlazeCelebrationScreenShown).thenReturn(false)

        createViewModel(isPostCampaignCreation = true)
        val state = viewModel.state.runAndCaptureValues {
            viewModel.onCampaignCelebrationDismissed()
        }.last()

        Assertions.assertThat(state.isCampaignCelebrationShown).isFalse()
    }

    @Test
    fun `when screen is not opened post creation, don't show celebration`() = testBlocking {
        createViewModel(isPostCampaignCreation = false)

        val state = viewModel.state.captureValues().last()

        Assertions.assertThat(state.isCampaignCelebrationShown).isFalse()
    }

    private fun createViewModel(isPostCampaignCreation: Boolean = false) {
        viewModel = BlazeCampaignListViewModel(
            savedStateHandle = BlazeCampaignListFragmentArgs(isPostCampaignCreation).toSavedStateHandle(),
            blazeCampaignsStore = blazeCampaignsStore,
            selectedSite = selectedSite,
            blazeUrlsHelper = blazeUrlsHelper,
            appPrefsWrapper = appPrefsWrapper,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            currencyFormatter = currencyFormatter
        )
    }

    private companion object {
        const val CAMPAIGN_ID = "1234"
        const val TITLE = "title"
        const val IMAGE_URL = "imageUrl"
        const val CREATED_AT = "2023-06-02T00:00:00.000Z"
        const val DURATION_DAYS = 10
        const val UI_STATUS = "rejected"
        const val IMPRESSIONS = 0L
        const val CLICKS = 0L
        const val TOTAL_BUDGET = 100.0
        const val SPENT_BUDGET = 0.0
        const val TARGET_URN = "urn:wpcom:post:199247490:9"

        val BLAZE_CAMPAIGN_MODEL = BlazeCampaignModel(
            campaignId = CAMPAIGN_ID,
            title = TITLE,
            imageUrl = IMAGE_URL,
            startTime = BlazeCampaignsUtils.stringToDate(CREATED_AT),
            durationInDays = DURATION_DAYS,
            uiStatus = UI_STATUS,
            impressions = IMPRESSIONS,
            clicks = CLICKS,
            targetUrn = TARGET_URN,
            totalBudget = TOTAL_BUDGET,
            spentBudget = SPENT_BUDGET,
            isEndlessCampaign = false
        )
        val EMPTY_BLAZE_CAMPAIGN_MODEL = BlazeCampaignsModel(
            campaigns = listOf(BLAZE_CAMPAIGN_MODEL),
            skipped = 0,
            totalItems = 1,
        )
        val BLAZE_CAMPAIGN_MODEL_1_OUT_OF_2_ITEMS = EMPTY_BLAZE_CAMPAIGN_MODEL
            .copy(
                campaigns = listOf(BLAZE_CAMPAIGN_MODEL.copy(campaignId = "1")),
                totalItems = 2
            )
    }
}
