package com.woocommerce.android

import com.woocommerce.android.config.WPComMobileActivePluginVersionsProvider
import com.woocommerce.android.config.WPComRemoteFeatureFlagRepository
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.environment.EnvironmentRepository
import com.woocommerce.android.util.GetAppVersionName
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.wear.WearableConnectionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class SiteObserverTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val environmentRepository: EnvironmentRepository = mock {
        on { fetchOrGetStoreID(any()) } doReturn Result.success("storeID")
    }
    private val wearableConnectionRepository: WearableConnectionRepository = mock()
    private val featureFlagRepository: WPComRemoteFeatureFlagRepository = mock()
    private val siteStore: SiteStore = mock()
    private val appPrefs: AppPrefsWrapper = mock()
    private val dispatcher: FakeDispatcher = FakeDispatcher()
    private val appVersionName: GetAppVersionName = mock()
    private val activePluginVersionsProvider = WPComMobileActivePluginVersionsProvider()

    private val siteObserver = SiteObserver(
        selectedSite = selectedSite,
        wooCommerceStore = wooCommerceStore,
        environmentRepository = environmentRepository,
        wearableConnectionRepository = wearableConnectionRepository,
        featureFlagRepository = featureFlagRepository,
        siteStore = siteStore,
        appPrefs = appPrefs,
        analyticsTracker = mock(),
        dispatcher = dispatcher,
        appVersionName = appVersionName,
        activePluginVersionsProvider = activePluginVersionsProvider
    )

    @Test
    fun `given app password connection, when starting observing, then fetch WPCom connect site info`() = testBlocking {
        val site = SiteModel().apply {
            id = SITE_LOCAL_ID.value
            url = "https://example.com"
            origin = SiteModel.ORIGIN_WPAPI
        }
        whenever(selectedSite.observe()).thenReturn(flowOf(site))
        whenever(siteStore.fetchConnectSiteInfoSync(site.url)).thenReturn(
            SiteStore.ConnectSiteInfoPayload(url = site.url)
        )
        whenever(wooCommerceStore.fetchSitePlugins(site)).thenReturn(WooResult(emptyList()))
        whenever(appVersionName()).thenReturn(APP_VERSION)

        val job = launch {
            siteObserver.observeAndUpdateSelectedSiteData()
        }

        verify(siteStore).fetchConnectSiteInfoSync(site.url)

        // Cancel the observer job
        job.cancel()
    }

    @Test
    fun `given app password connection, when site info is fetched, then update app flag`() =
        testBlocking {
            listOf(false, true).forEach { isSuspended ->
                val site = SiteModel().apply {
                    id = SITE_LOCAL_ID.value
                    url = "https://example.com"
                    origin = SiteModel.ORIGIN_WPAPI
                }
                whenever(selectedSite.observe()).thenReturn(flowOf(site))
                whenever(wooCommerceStore.fetchSitePlugins(site)).thenReturn(WooResult(emptyList()))
                whenever(appVersionName()).thenReturn(APP_VERSION)
                val connectSiteInfo = if (isSuspended) {
                    SiteStore.ConnectSiteInfoPayload(
                        error = SiteStore.SiteError(type = SiteStore.SiteErrorType.WPCOM_SITE_SUSPENDED),
                        url = site.url
                    )
                } else {
                    SiteStore.ConnectSiteInfoPayload(url = site.url)
                }
                whenever(siteStore.fetchConnectSiteInfoSync(site.url)).thenReturn(connectSiteInfo)

                val job = launch {
                    siteObserver.observeAndUpdateSelectedSiteData()
                }

                verify(appPrefs).isSiteWPComSuspended = isSuspended

                // Cancel the observer job
                job.cancel()
            }
        }

    @Test
    fun `given site with app password connection, when fetching site info fails, then don't update flag`() =
        testBlocking {
            val site = SiteModel().apply {
                id = SITE_LOCAL_ID.value
                url = "https://example.com"
                origin = SiteModel.ORIGIN_WPAPI
            }
            whenever(selectedSite.observe()).thenReturn(flowOf(site))
            whenever(wooCommerceStore.fetchSitePlugins(site)).thenReturn(WooResult(emptyList()))
            whenever(appVersionName()).thenReturn(APP_VERSION)
            whenever(siteStore.fetchConnectSiteInfoSync(site.url)).thenReturn(
                SiteStore.ConnectSiteInfoPayload(
                    error = SiteStore.SiteError(type = SiteStore.SiteErrorType.INVALID_SITE),
                    url = site.url
                )
            )

            val job = launch {
                siteObserver.observeAndUpdateSelectedSiteData()
            }

            verify(appPrefs, never()).isSiteWPComSuspended

            // Cancel the observer job
            job.cancel()
        }

    @Test
    fun `when observeAndUpdateSelectedSiteData is called, fetchRemoteFeatureFlags is called`() = testBlocking {
        // GIVEN
        val site = SiteModel().apply {
            id = SITE_LOCAL_ID.value
            url = "https://example.com"
            origin = SiteModel.ORIGIN_WPAPI
        }
        whenever(selectedSite.observe()).thenReturn(flowOf(site))
        whenever(siteStore.fetchConnectSiteInfoSync(site.url)).thenReturn(
            SiteStore.ConnectSiteInfoPayload(url = site.url)
        )
        whenever(wooCommerceStore.fetchSitePlugins(site)).thenReturn(WooResult(emptyList()))
        whenever(appVersionName()).thenReturn(APP_VERSION)

        // WHEN
        val job = launch {
            siteObserver.observeAndUpdateSelectedSiteData()
        }
        advanceUntilIdle()

        // THEN
        verify(featureFlagRepository).fetchAndCacheFeatureFlags(APP_VERSION, site.localId(), emptyMap())

        job.cancel()
    }

    @Test
    fun `when plugins are fetched successfully, then fetch remote feature flags with Woo plugin version`() = testBlocking {
        // GIVEN
        val site = SiteModel().apply {
            id = SITE_LOCAL_ID.value
            url = "https://example.com"
        }
        val plugins = listOf(
            SitePluginModel(
                siteId = site.localId(),
                name = WooCommerceStore.WooPlugin.WOO_CORE.pluginName,
                version = WOO_VERSION,
                slug = "woocommerce",
                authorName = "",
                isActive = true
            )
        )
        whenever(selectedSite.observe()).thenReturn(flowOf(site))
        whenever(siteStore.fetchConnectSiteInfoSync(site.url)).thenReturn(
            SiteStore.ConnectSiteInfoPayload(url = site.url)
        )
        whenever(wooCommerceStore.fetchSitePlugins(site)).thenReturn(WooResult(plugins))
        whenever(appVersionName()).thenReturn(APP_VERSION)

        // WHEN
        val job = launch {
            siteObserver.observeAndUpdateSelectedSiteData()
        }
        advanceUntilIdle()

        // THEN
        inOrder(wooCommerceStore, featureFlagRepository) {
            verify(wooCommerceStore).fetchSitePlugins(site)
            verify(featureFlagRepository).fetchAndCacheFeatureFlags(
                APP_VERSION,
                site.localId(),
                mapOf("woocommerce/woocommerce.php" to WOO_VERSION)
            )
        }

        job.cancel()
    }

    @Test
    fun `given plugin fetch fails, when observing site, then fetch remote feature flags with empty plugin map`() =
        testBlocking {
            // GIVEN
            val site = SiteModel().apply {
                id = SITE_LOCAL_ID.value
                url = "https://example.com"
            }
            whenever(selectedSite.observe()).thenReturn(flowOf(site))
            whenever(siteStore.fetchConnectSiteInfoSync(site.url)).thenReturn(
                SiteStore.ConnectSiteInfoPayload(url = site.url)
            )
            whenever(wooCommerceStore.fetchSitePlugins(site)).thenReturn(
                WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            )
            whenever(appVersionName()).thenReturn(APP_VERSION)

            // WHEN
            val job = launch {
                siteObserver.observeAndUpdateSelectedSiteData()
            }
            advanceUntilIdle()

            // THEN
            verify(featureFlagRepository).fetchAndCacheFeatureFlags(APP_VERSION, site.localId(), emptyMap())

            job.cancel()
        }

    private companion object {
        val SITE_LOCAL_ID = LocalId(1)
        const val APP_VERSION = "1.0.0"
        const val WOO_VERSION = "10.9.2"
    }
}
