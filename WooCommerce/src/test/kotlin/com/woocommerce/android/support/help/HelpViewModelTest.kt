package com.woocommerce.android.support.help

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.support.TicketType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class HelpViewModelTest : BaseUnitTest() {
    private val savedState: SavedStateHandle = SavedStateHandle()
    private val selectedSite: SelectedSite = mock {
        whenever(it.get()).thenReturn(mock())
    }
    private val wooStore: WooCommerceStore = mock()
    private val viewModel = HelpViewModel(
        savedState,
        wooStore,
        selectedSite,
    )

    @Test
    fun `when on contact clicked, then loading event triggered`() {
        // WHEN
        viewModel.contactSupport(TicketType.General)

        // THEN
        assertThat(viewModel.event.value).isEqualTo(HelpViewModel.ContactPaymentsSupportClickEvent.ShowLoading)
    }

    @Test
    fun `given woo store returns error general, when on contact clicked, then create event triggered with error tag`() =
        testBlocking {
            // GIVEN
            whenever(wooStore.fetchSitePlugins(any())).thenReturn(
                WooResult(
                    WooError(
                        WooErrorType.API_ERROR,
                        BaseRequest.GenericErrorType.NETWORK_ERROR
                    )
                )
            )

            // WHEN
            viewModel.contactSupport(TicketType.General)

            // THEN
            assertThat(viewModel.event.value).isEqualTo(
                HelpViewModel.ContactPaymentsSupportClickEvent.CreateTicket(
                    TicketType.General,
                    listOf("woo_android_site_plugins_fetching_error")
                )
            )
        }

    @Test
    fun `given woo store returns error pay, when on contact clicked, then create event triggered with error tag`() =
        testBlocking {
            // GIVEN
            whenever(wooStore.fetchSitePlugins(any())).thenReturn(
                WooResult(
                    WooError(
                        WooErrorType.API_ERROR,
                        BaseRequest.GenericErrorType.NETWORK_ERROR
                    )
                )
            )

            // WHEN
            viewModel.contactSupport(TicketType.Payments)

            // THEN
            assertThat(viewModel.event.value).isEqualTo(
                HelpViewModel.ContactPaymentsSupportClickEvent.CreateTicket(
                    TicketType.Payments,
                    listOf("woo_android_site_plugins_fetching_error")
                )
            )
        }
}
