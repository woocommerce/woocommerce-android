package com.woocommerce.android.tracker

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class TrackStoreSnapshotTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val selectedSiteModel: SiteModel = mock {
        on { id }.thenReturn(1)
        on { siteId }.thenReturn(2L)
        on { selfHostedSiteId }.thenReturn(3L)
    }
    private val tracker: AnalyticsTrackerWrapper = mock()
    private val appPrefs: AppPrefs = mock()
    private val productStore: WCProductStore = mock()
    private val ordersStore: WCOrderStore = mock()
    private val wooStore: WooCommerceStore = mock()

    private val trackStoreSnapshot = TrackStoreSnapshot(
        selectedSite = selectedSite,
        tracker = tracker,
        appPrefs = appPrefs,
        productStore = productStore,
        ordersStore = ordersStore,
        wooStore = wooStore,
    )

    @Test
    fun `given site does not exist, when invoke, then do nothing`() = testBlocking {
        // GIVEN
        whenever(selectedSite.getIfExists()).thenReturn(null)

        // WHEN
        trackStoreSnapshot()

        // THEN
        verifyNoInteractions(tracker)
        verifyNoInteractions(appPrefs)
        verifyNoInteractions(productStore)
        verifyNoInteractions(ordersStore)
        verifyNoInteractions(wooStore)
    }

    @Test
    fun `given site exists and tracked for site before, when invoke, then do nothing`() = testBlocking {
        // GIVEN
        whenever(selectedSite.getIfExists()).thenReturn(selectedSiteModel)
        whenever(
            appPrefs.isApplicationStoreSnapshotTrackedForSite(
                localSiteId = 1,
                remoteSiteId = 2L,
                selfHostedSiteId = 3L
            )
        ).thenReturn(true)

        // WHEN
        trackStoreSnapshot()

        // THEN
        verifyNoInteractions(tracker)
        verifyNoInteractions(productStore)
        verifyNoInteractions(ordersStore)
        verifyNoInteractions(wooStore)
    }

    @Test
    fun `given site exists and not tracked for site before and orders returns error, when invoke, then do not track event`() =
        testBlocking {
            // GIVEN
            whenever(selectedSite.getIfExists()).thenReturn(selectedSiteModel)
            whenever(
                appPrefs.isApplicationStoreSnapshotTrackedForSite(
                    localSiteId = 1,
                    remoteSiteId = 2L,
                    selfHostedSiteId = 3L
                )
            ).thenReturn(false)
            whenever(productStore.fetchProductsCount(selectedSiteModel)).thenReturn(WooResult(1))
            whenever(ordersStore.fetchOrdersCount(selectedSiteModel)).thenReturn(
                WCOrderStore.OrdersCountResult.Failure(
                    WCOrderStore.OrderError()
                )
            )
            whenever(wooStore.fetchSitePlugins(selectedSiteModel)).thenReturn(
                WooResult(emptyList())
            )

            // WHEN
            trackStoreSnapshot()

            // THEN
            verifyNoInteractions(tracker)
        }

    @Test
    fun `given site exists and not tracked for site before and products returns error, when invoke, then do not track event`() =
        testBlocking {
            // GIVEN
            whenever(selectedSite.getIfExists()).thenReturn(selectedSiteModel)
            whenever(
                appPrefs.isApplicationStoreSnapshotTrackedForSite(
                    localSiteId = 1,
                    remoteSiteId = 2L,
                    selfHostedSiteId = 3L
                )
            ).thenReturn(false)
            whenever(productStore.fetchProductsCount(selectedSiteModel)).thenReturn(
                WooResult(WooError(WooErrorType.API_ERROR, BaseRequest.GenericErrorType.NETWORK_ERROR))
            )
            whenever(ordersStore.fetchOrdersCount(selectedSiteModel)).thenReturn(
                WCOrderStore.OrdersCountResult.Success(1)
            )
            whenever(wooStore.fetchSitePlugins(selectedSiteModel)).thenReturn(
                WooResult(emptyList())
            )

            // WHEN
            trackStoreSnapshot()

            // THEN
            verifyNoInteractions(tracker)
        }

    @Test
    fun `given site exists and not tracked for site before and plugins returns error, when invoke, then do not track event`() =
        testBlocking {
            // GIVEN
            whenever(selectedSite.getIfExists()).thenReturn(selectedSiteModel)
            whenever(
                appPrefs.isApplicationStoreSnapshotTrackedForSite(
                    localSiteId = 1,
                    remoteSiteId = 2L,
                    selfHostedSiteId = 3L
                )
            ).thenReturn(false)
            whenever(productStore.fetchProductsCount(selectedSiteModel)).thenReturn(WooResult(1))
            whenever(ordersStore.fetchOrdersCount(selectedSiteModel)).thenReturn(
                WCOrderStore.OrdersCountResult.Success(1)
            )
            whenever(wooStore.fetchSitePlugins(selectedSiteModel)).thenReturn(
                WooResult(WooError(WooErrorType.API_ERROR, BaseRequest.GenericErrorType.NETWORK_ERROR))
            )

            // WHEN
            trackStoreSnapshot()

            // THEN
            verifyNoInteractions(tracker)
        }

    @Test
    fun `given no plugins returned, when invoke, then track event with not_installed`() =
        testBlocking {
            // GIVEN
            whenever(selectedSite.getIfExists()).thenReturn(selectedSiteModel)
            whenever(
                appPrefs.isApplicationStoreSnapshotTrackedForSite(
                    localSiteId = 1,
                    remoteSiteId = 2L,
                    selfHostedSiteId = 3L
                )
            ).thenReturn(false)
            whenever(productStore.fetchProductsCount(selectedSiteModel)).thenReturn(WooResult(1))
            whenever(ordersStore.fetchOrdersCount(selectedSiteModel)).thenReturn(
                WCOrderStore.OrdersCountResult.Success(2)
            )
            whenever(wooStore.fetchSitePlugins(selectedSiteModel)).thenReturn(
                WooResult(emptyList())
            )

            // WHEN
            trackStoreSnapshot()

            // THEN
            verify(tracker).track(
                AnalyticsEvent.APPLICATION_STORE_SNAPSHOT,
                mapOf(
                    "products_count" to 1L,
                    "orders_count" to 2,
                    "woocommerce_payments" to "not_installed",
                    "woocommerce_gateway_stripe" to "not_installed",
                    "woocommerce_gateway_stripe" to "not_installed",
                    "woocommerce_square" to "not_installed",
                    "woocommerce_paypal_payments" to "not_installed",
                )
            )
        }

    @Test
    fun `given plugins returned not activated, when invoke, then track event with installed_and_not_activated`() =
        testBlocking {
            // GIVEN
            whenever(selectedSite.getIfExists()).thenReturn(selectedSiteModel)
            whenever(
                appPrefs.isApplicationStoreSnapshotTrackedForSite(
                    localSiteId = 1,
                    remoteSiteId = 2L,
                    selfHostedSiteId = 3L
                )
            ).thenReturn(false)
            whenever(productStore.fetchProductsCount(selectedSiteModel)).thenReturn(WooResult(1))
            whenever(ordersStore.fetchOrdersCount(selectedSiteModel)).thenReturn(
                WCOrderStore.OrdersCountResult.Success(2)
            )
            whenever(wooStore.fetchSitePlugins(selectedSiteModel)).thenReturn(
                WooResult(
                    listOf(
                        SitePluginModel().apply {
                            name = "woocommerce-payments"
                            setIsActive(false)
                        },
                        SitePluginModel().apply {
                            name = "woocommerce-gateway-stripe"
                            setIsActive(false)
                        },
                        SitePluginModel().apply {
                            name = "woocommerce-square"
                            setIsActive(false)
                        },
                        SitePluginModel().apply {
                            name = "woocommerce-paypal-payments"
                            setIsActive(false)
                        },
                    )
                )
            )

            // WHEN
            trackStoreSnapshot()

            // THEN
            verify(tracker).track(
                AnalyticsEvent.APPLICATION_STORE_SNAPSHOT,
                mapOf(
                    "products_count" to 1L,
                    "orders_count" to 2,
                    "woocommerce_payments" to "installed_and_not_activated",
                    "woocommerce_gateway_stripe" to "installed_and_not_activated",
                    "woocommerce_gateway_stripe" to "installed_and_not_activated",
                    "woocommerce_square" to "installed_and_not_activated",
                    "woocommerce_paypal_payments" to "installed_and_not_activated",
                )
            )
        }

    @Test
    fun `given plugins returned activated, when invoke, then track event with installed_and_activated`() =
        testBlocking {
            // GIVEN
            whenever(selectedSite.getIfExists()).thenReturn(selectedSiteModel)
            whenever(
                appPrefs.isApplicationStoreSnapshotTrackedForSite(
                    localSiteId = 1,
                    remoteSiteId = 2L,
                    selfHostedSiteId = 3L
                )
            ).thenReturn(false)
            whenever(productStore.fetchProductsCount(selectedSiteModel)).thenReturn(WooResult(1))
            whenever(ordersStore.fetchOrdersCount(selectedSiteModel)).thenReturn(
                WCOrderStore.OrdersCountResult.Success(2)
            )
            whenever(wooStore.fetchSitePlugins(selectedSiteModel)).thenReturn(
                WooResult(
                    listOf(
                        SitePluginModel().apply {
                            name = "woocommerce-payments"
                            setIsActive(true)
                        },
                        SitePluginModel().apply {
                            name = "woocommerce-gateway-stripe"
                            setIsActive(true)
                        },
                        SitePluginModel().apply {
                            name = "woocommerce-square"
                            setIsActive(true)
                        },
                        SitePluginModel().apply {
                            name = "woocommerce-paypal-payments"
                            setIsActive(true)
                        },
                    )
                )
            )

            // WHEN
            trackStoreSnapshot()

            // THEN
            verify(tracker).track(
                AnalyticsEvent.APPLICATION_STORE_SNAPSHOT,
                mapOf(
                    "products_count" to 1L,
                    "orders_count" to 2,
                    "woocommerce_payments" to "installed_and_activated",
                    "woocommerce_gateway_stripe" to "installed_and_activated",
                    "woocommerce_gateway_stripe" to "installed_and_activated",
                    "woocommerce_square" to "installed_and_activated",
                    "woocommerce_paypal_payments" to "installed_and_activated",
                )
            )
        }

    @Test
    fun `given plugins returned but name doesnt match, when invoke, then track event with not_installed`() =
        testBlocking {
            // GIVEN
            whenever(selectedSite.getIfExists()).thenReturn(selectedSiteModel)
            whenever(
                appPrefs.isApplicationStoreSnapshotTrackedForSite(
                    localSiteId = 1,
                    remoteSiteId = 2L,
                    selfHostedSiteId = 3L
                )
            ).thenReturn(false)
            whenever(productStore.fetchProductsCount(selectedSiteModel)).thenReturn(WooResult(1))
            whenever(ordersStore.fetchOrdersCount(selectedSiteModel)).thenReturn(
                WCOrderStore.OrdersCountResult.Success(2)
            )
            whenever(wooStore.fetchSitePlugins(selectedSiteModel)).thenReturn(
                WooResult(
                    listOf(
                        SitePluginModel().apply {
                            name = "woocommerce-payments"
                            setIsActive(true)
                        },
                        SitePluginModel().apply {
                            name = "woocommerce-gateway-stripe"
                            setIsActive(true)
                        },
                        SitePluginModel().apply {
                            name = "woocommerce-square"
                            setIsActive(true)
                        },
                        SitePluginModel().apply {
                            name = "woocommerce-paypal-payments-1"
                            setIsActive(true)
                        },
                    )
                )
            )

            // WHEN
            trackStoreSnapshot()

            // THEN
            verify(tracker).track(
                AnalyticsEvent.APPLICATION_STORE_SNAPSHOT,
                mapOf(
                    "products_count" to 1L,
                    "orders_count" to 2,
                    "woocommerce_payments" to "installed_and_activated",
                    "woocommerce_gateway_stripe" to "installed_and_activated",
                    "woocommerce_gateway_stripe" to "installed_and_activated",
                    "woocommerce_square" to "installed_and_activated",
                    "woocommerce_paypal_payments" to "not_installed",
                )
            )
        }
}
