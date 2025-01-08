package com.woocommerce.android.ui.blaze.detail

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper
import com.woocommerce.android.ui.blaze.detail.BlazeCampaignDetailWebViewViewModel.BlazeAction.CampaignStopped
import com.woocommerce.android.ui.blaze.detail.BlazeCampaignDetailWebViewViewModel.BlazeAction.PromoteProductAgain
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class BlazeCampaignDetailWebViewViewModelTest : BaseUnitTest() {
    companion object {
        private const val SITE_URL = "wordpresssite.com"
        private const val CAMPAIGN_ID = "1"
        private const val PRODUCT_ID = "2"
        private const val DISMISS_URL = "${BlazeUrlsHelper.BASE_URL}/campaigns/$SITE_URL"
        private const val STOP_URL = "/campaigns/$CAMPAIGN_ID/stop"
        private const val PROMOTE_AGAIN_URL = "/${BlazeUrlsHelper.PROMOTE_AGAIN_URL_PATH}-$PRODUCT_ID"
    }

    private val siteModel: SiteModel = mock {
        on { url }.thenReturn(SITE_URL)
    }
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(siteModel)
    }
    private val savedStateHandle = BlazeCampaignDetailWebViewFragmentArgs(CAMPAIGN_ID).toSavedStateHandle()
    private val blazeUrlsHelper: BlazeUrlsHelper = BlazeUrlsHelper(selectedSite)

    private val viewModel = BlazeCampaignDetailWebViewViewModel(savedStateHandle, blazeUrlsHelper)

    @Test
    fun `given no action was performed, when navigating back, trigger exit`() = testBlocking {
        viewModel.onUrlLoaded(DISMISS_URL)
        val exitEvent = viewModel.event.captureValues().filterIsInstance<Event.Exit>().last()

        assertThat(exitEvent).isEqualTo(Event.Exit)
    }

    @Test
    fun `given campaign is stopped, when navigating back, trigger CampaignStopped action`() = testBlocking {
        viewModel.onUrlLoaded(STOP_URL)

        viewModel.onDismiss()
        val exitResultEvent = viewModel.event.captureValues().last() as Event.ExitWithResult<*>

        assertThat(exitResultEvent.data).isEqualTo(CampaignStopped)
    }

    @Test
    fun `when promote again tapped, trigger PromoteProductAgain action`() = testBlocking {
        viewModel.onUrlLoaded(PROMOTE_AGAIN_URL)

        val exitResultEvent = viewModel.event.captureValues().last() as Event.ExitWithResult<*>

        assertThat(exitResultEvent.data).isEqualTo(PromoteProductAgain(productId = PRODUCT_ID.toLong()))
    }
}
