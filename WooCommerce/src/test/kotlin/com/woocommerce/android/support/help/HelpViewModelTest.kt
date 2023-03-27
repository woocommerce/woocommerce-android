package com.woocommerce.android.support.help

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.support.zendesk.TicketType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSSRModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class HelpViewModelTest : BaseUnitTest() {
    private val savedState: SavedStateHandle = SavedStateHandle()
    private val siteModel: SiteModel = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(siteModel)
        on { exists() }.thenReturn(true)
    }
    private val wooStore: WooCommerceStore = mock {
        onBlocking { fetchSSR(any()) }.thenReturn(WooResult(WooError(mock(), mock())))
    }

    private lateinit var viewModel: HelpViewModel

    @Before
    fun initViewModel() {
        viewModel = HelpViewModel(
            savedState,
            wooStore,
            selectedSite,
            mock()
        )
    }

    @Test
    fun `given site doesnt exist, when on contact clicked, then create event triggered`() {
        // GIVEN
        whenever(selectedSite.exists()).thenReturn(false)

        // WHEN
        viewModel.contactSupport(TicketType.MobileApp)

        // THEN
        assertThat(viewModel.event.value).isEqualTo(
            HelpViewModel.ContactSupportEvent.CreateTicket(
                TicketType.MobileApp,
                emptyList(),
            )
        )
    }

    @Test
    fun `given site exists, when on contact clicked, then loading event triggered`() {
        // GIVEN
        whenever(selectedSite.exists()).thenReturn(true)

        // WHEN
        viewModel.contactSupport(TicketType.MobileApp)

        // THEN
        assertThat(viewModel.event.value).isEqualTo(HelpViewModel.ContactSupportEvent.ShowLoading)
    }

    @Test
    fun `given woo store returns error general, when on contact clicked, then create event triggered with error tag`() =
        testBlocking {
            // GIVEN
            whenever(wooStore.fetchSitePlugins(siteModel)).thenReturn(
                WooResult(
                    WooError(
                        WooErrorType.API_ERROR,
                        BaseRequest.GenericErrorType.NETWORK_ERROR
                    )
                )
            )

            // WHEN
            viewModel.contactSupport(TicketType.MobileApp)

            // THEN
            assertThat(viewModel.event.value).isEqualTo(
                HelpViewModel.ContactSupportEvent.CreateTicket(
                    TicketType.MobileApp,
                    listOf("woo_mobile_site_plugins_fetching_error")
                )
            )
        }

    @Test
    fun `given woo store returns error pay, when on contact clicked, then create event triggered with error tag`() =
        testBlocking {
            // GIVEN
            whenever(wooStore.fetchSitePlugins(siteModel)).thenReturn(
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
                HelpViewModel.ContactSupportEvent.CreateTicket(
                    TicketType.Payments,
                    listOf("woo_mobile_site_plugins_fetching_error")
                )
            )
        }

    @Test
    fun `given store success with no plugins, when on contact clicked, then event triggered with no plugins tag`() =
        testBlocking {
            // GIVEN
            whenever(wooStore.fetchSitePlugins(siteModel)).thenReturn(WooResult(emptyList()))
            whenever(wooStore.getSitePlugin(any(), any())).thenReturn(null)

            // WHEN
            viewModel.contactSupport(TicketType.MobileApp)

            // THEN
            assertThat(viewModel.event.value).isEqualTo(
                HelpViewModel.ContactSupportEvent.CreateTicket(
                    TicketType.MobileApp,
                    listOf(
                        "woo_mobile_wcpay_not_installed",
                        "woo_mobile_stripe_not_installed",
                    )
                )
            )
        }

    @Test
    fun `given store success with wcpay installed, when on contact clicked, then event triggered with wcpay tag`() =
        testBlocking {
            // GIVEN
            whenever(wooStore.fetchSitePlugins(siteModel)).thenReturn(WooResult(emptyList()))
            whenever(wooStore.getSitePlugin(siteModel, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
                .thenReturn(mock())

            // WHEN
            viewModel.contactSupport(TicketType.MobileApp)

            // THEN
            assertThat(viewModel.event.value).isEqualTo(
                HelpViewModel.ContactSupportEvent.CreateTicket(
                    TicketType.MobileApp,
                    listOf(
                        "woo_mobile_wcpay_installed_and_not_activated",
                        "woo_mobile_stripe_not_installed",
                    )
                )
            )
        }

    @Test
    fun `given store success with wcpay installed and act, when on contact clicked, then event triggered wcpay tag`() =
        testBlocking {
            // GIVEN
            whenever(wooStore.fetchSitePlugins(siteModel)).thenReturn(WooResult(emptyList()))
            val wcpayPluginModel = mock<SitePluginModel> {
                on { isActive }.thenReturn(true)
            }
            whenever(wooStore.getSitePlugin(siteModel, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
                .thenReturn(wcpayPluginModel)

            // WHEN
            viewModel.contactSupport(TicketType.MobileApp)

            // THEN
            assertThat(viewModel.event.value).isEqualTo(
                HelpViewModel.ContactSupportEvent.CreateTicket(
                    TicketType.MobileApp,
                    listOf(
                        "woo_mobile_wcpay_installed_and_activated",
                        "woo_mobile_stripe_not_installed",
                    )
                )
            )
        }

    @Test
    fun `given store success with stripe installed, when on contact clicked, then event triggered with stripe tag`() =
        testBlocking {
            // GIVEN
            whenever(wooStore.fetchSitePlugins(siteModel)).thenReturn(WooResult(emptyList()))
            whenever(wooStore.getSitePlugin(siteModel, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
                .thenReturn(mock())

            // WHEN
            viewModel.contactSupport(TicketType.MobileApp)

            // THEN
            assertThat(viewModel.event.value).isEqualTo(
                HelpViewModel.ContactSupportEvent.CreateTicket(
                    TicketType.MobileApp,
                    listOf(
                        "woo_mobile_wcpay_not_installed",
                        "woo_mobile_stripe_installed_and_not_activated",
                    )
                )
            )
        }

    @Test
    fun `given store success with stripe ins and act, when on contact clicked, then event triggered with tag`() =
        testBlocking {
            // GIVEN
            whenever(wooStore.fetchSitePlugins(siteModel)).thenReturn(WooResult(emptyList()))
            val stripePluginModel = mock<SitePluginModel> {
                on { isActive }.thenReturn(true)
            }
            whenever(wooStore.getSitePlugin(siteModel, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
                .thenReturn(stripePluginModel)

            // WHEN
            viewModel.contactSupport(TicketType.MobileApp)

            // THEN
            assertThat(viewModel.event.value).isEqualTo(
                HelpViewModel.ContactSupportEvent.CreateTicket(
                    TicketType.MobileApp,
                    listOf(
                        "woo_mobile_wcpay_not_installed",
                        "woo_mobile_stripe_installed_and_activated",
                    )
                )
            )
        }

    @Test
    fun `given store success with wc and stripe installed, when on contact clicked, then event triggered tag`() =
        testBlocking {
            // GIVEN
            whenever(wooStore.fetchSitePlugins(siteModel)).thenReturn(WooResult(emptyList()))
            val wcpayPluginModel = mock<SitePluginModel>()
            whenever(wooStore.getSitePlugin(siteModel, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
                .thenReturn(wcpayPluginModel)
            val stripePluginModel = mock<SitePluginModel>()
            whenever(wooStore.getSitePlugin(siteModel, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
                .thenReturn(stripePluginModel)

            // WHEN
            viewModel.contactSupport(TicketType.MobileApp)

            // THEN
            assertThat(viewModel.event.value).isEqualTo(
                HelpViewModel.ContactSupportEvent.CreateTicket(
                    TicketType.MobileApp,
                    listOf(
                        "woo_mobile_wcpay_installed_and_not_activated",
                        "woo_mobile_stripe_installed_and_not_activated",
                    )
                )
            )
        }

    @Test
    fun `given store success with wc inst and act and stripe ins, when on contact clicked, then event triggered tag`() =
        testBlocking {
            // GIVEN
            whenever(wooStore.fetchSitePlugins(siteModel)).thenReturn(WooResult(emptyList()))
            val wcpayPluginModel = mock<SitePluginModel> {
                on { isActive }.thenReturn(true)
            }
            whenever(wooStore.getSitePlugin(siteModel, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
                .thenReturn(wcpayPluginModel)
            val stripePluginModel = mock<SitePluginModel>()
            whenever(wooStore.getSitePlugin(siteModel, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
                .thenReturn(stripePluginModel)

            // WHEN
            viewModel.contactSupport(TicketType.MobileApp)

            // THEN
            assertThat(viewModel.event.value).isEqualTo(
                HelpViewModel.ContactSupportEvent.CreateTicket(
                    TicketType.MobileApp,
                    listOf(
                        "woo_mobile_wcpay_installed_and_activated",
                        "woo_mobile_stripe_installed_and_not_activated",
                    )
                )
            )
        }

    @Test
    fun `given store success with wc inst and stripe ins and act, when on contact clicked, then event triggered tag`() =
        testBlocking {
            // GIVEN
            whenever(wooStore.fetchSitePlugins(siteModel)).thenReturn(WooResult(emptyList()))
            val wcpayPluginModel = mock<SitePluginModel>()
            whenever(wooStore.getSitePlugin(siteModel, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
                .thenReturn(wcpayPluginModel)
            val stripePluginModel = mock<SitePluginModel> {
                on { isActive }.thenReturn(true)
            }
            whenever(wooStore.getSitePlugin(siteModel, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
                .thenReturn(stripePluginModel)

            // WHEN
            viewModel.contactSupport(TicketType.MobileApp)

            // THEN
            assertThat(viewModel.event.value).isEqualTo(
                HelpViewModel.ContactSupportEvent.CreateTicket(
                    TicketType.MobileApp,
                    listOf(
                        "woo_mobile_wcpay_installed_and_not_activated",
                        "woo_mobile_stripe_installed_and_activated",
                    )
                )
            )
        }

    @Test
    fun `given store success with wc and stripe inst and act, when on contact clicked, then event triggered tag`() =
        testBlocking {
            // GIVEN
            whenever(wooStore.fetchSitePlugins(siteModel)).thenReturn(WooResult(emptyList()))
            val wcpayPluginModel = mock<SitePluginModel> {
                on { isActive }.thenReturn(true)
            }
            whenever(wooStore.getSitePlugin(siteModel, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
                .thenReturn(wcpayPluginModel)
            val stripePluginModel = mock<SitePluginModel> {
                on { isActive }.thenReturn(true)
            }
            whenever(wooStore.getSitePlugin(siteModel, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
                .thenReturn(stripePluginModel)

            // WHEN
            viewModel.contactSupport(TicketType.MobileApp)

            // THEN
            assertThat(viewModel.event.value).isEqualTo(
                HelpViewModel.ContactSupportEvent.CreateTicket(
                    TicketType.MobileApp,
                    listOf(
                        "woo_mobile_wcpay_installed_and_activated",
                        "woo_mobile_stripe_installed_and_activated",
                    )
                )
            )
        }

    @Test
    fun `given site doesnt exist, when view model init, then ssr is null`() {
        // GIVEN
        whenever(selectedSite.exists()).thenReturn(false)

        // WHEN
        viewModel = HelpViewModel(
            savedState,
            wooStore,
            selectedSite,
            mock()
        )

        // THEN
        assertThat(viewModel.ssr).isNull()
    }

    @Test
    fun `given site exists and fetch ssr success, when view model init, then ssr is not null`() = runTest {
        // GIVEN
        whenever(selectedSite.exists()).thenReturn(true)
        whenever(wooStore.fetchSSR(any())).thenReturn(WooResult(WCSSRModel(remoteSiteId = 1)))

        // WHEN
        viewModel = HelpViewModel(
            savedState,
            wooStore,
            selectedSite,
            mock()
        )

        // THEN
        assertThat(viewModel.ssr).isNotNull
    }

    @Test
    fun `given site exists and fetch ssr error, when view model init, then ssr is null`() {
        // GIVEN
        whenever(selectedSite.exists()).thenReturn(true)

        // WHEN
        viewModel = HelpViewModel(
            savedState,
            wooStore,
            selectedSite,
            mock()
        )

        // THEN
        assertThat(viewModel.ssr).isNull()
    }
}
