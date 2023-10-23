package com.woocommerce.android.ui.blaze.campaigs

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignsModel
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsError
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao.BlazeCampaignEntity
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore.BlazeCampaignsResult

@ExperimentalCoroutinesApi
class BlazeCampaignListViewModelTest : BaseUnitTest() {
    private val blazeCampaignsStore: BlazeCampaignsStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val blazeUrlsHelper: BlazeUrlsHelper = mock()
    private val siteModel: SiteModel = mock()
    private val campaignsEntityFlow = flow { emit(emptyList<BlazeCampaignEntity>()) }
    private lateinit var viewModel: BlazeCampaignListViewModel

    @Before
    fun setup() {
        whenever(selectedSite.get()).thenReturn(siteModel)
        whenever(blazeCampaignsStore.observeBlazeCampaigns(selectedSite.get())).thenReturn(campaignsEntityFlow)
    }

    @Test
    fun `when screen loaded, subscribe to blaze campaigns changes`() = testBlocking {
        createViewModel()

        verify(blazeCampaignsStore).observeBlazeCampaigns(siteModel)
    }

    @Test
    fun `when screen loaded, fetch campaigns for first page`() = testBlocking {
        whenever(blazeCampaignsStore.fetchBlazeCampaigns(any(), any()))
            .thenReturn(BlazeCampaignsResult(EMPTY_BLAZE_CAMPAIGN_MODEL))

        createViewModel()

        verify(blazeCampaignsStore).fetchBlazeCampaigns(
            siteModel,
            page = 1
        )
    }

    @Test
    fun `given more than 2 pages of campaigns, when reaching end of the list, fetch next page`() =
        testBlocking {
            whenever(blazeCampaignsStore.fetchBlazeCampaigns(any(), any()))
                .thenReturn(BlazeCampaignsResult(BLAZE_CAMPAIGN_MODEL_2_PAGES))

            createViewModel()

            viewModel.onEndOfTheListReached()
            val uiState = viewModel.state.captureValues().last()

            verify(blazeCampaignsStore).fetchBlazeCampaigns(
                siteModel,
                page = 2
            )
            Assertions.assertThat(uiState.isLoading).isEqualTo(false)
        }

    @Test
    fun `given fetching campaigns fails, when reaching end of the list, then show error snackbar`() = testBlocking {
        whenever(blazeCampaignsStore.fetchBlazeCampaigns(any(), any()))
            .thenReturn(BlazeCampaignsResult(BlazeCampaignsError(INVALID_RESPONSE)))
        createViewModel()

        viewModel.onEndOfTheListReached()
        val errorEvent = viewModel.event.captureValues().filterIsInstance<Event.ShowSnackbar>().last()

        Assertions.assertThat(errorEvent.message).isEqualTo(R.string.blaze_campaign_list_error_fetching_campaigns)
    }

    private fun createViewModel() {
        viewModel = BlazeCampaignListViewModel(
            savedStateHandle = SavedStateHandle(),
            blazeCampaignsStore = blazeCampaignsStore,
            selectedSite = selectedSite,
            blazeUrlsHelper = blazeUrlsHelper
        )
    }

    private companion object {
        val EMPTY_BLAZE_CAMPAIGN_MODEL = BlazeCampaignsModel(
            campaigns = emptyList(),
            page = 1,
            totalItems = 1,
            totalPages = 1
        )
        val BLAZE_CAMPAIGN_MODEL_2_PAGES = EMPTY_BLAZE_CAMPAIGN_MODEL
            .copy(totalPages = 2)
    }
}
