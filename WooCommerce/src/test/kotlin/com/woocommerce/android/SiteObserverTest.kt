package com.woocommerce.android

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.environment.EnvironmentRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.wear.WearableConnectionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SiteObserverTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val environmentRepository: EnvironmentRepository = mock {
        onBlocking { fetchOrGetStoreID(any()) } doReturn WooResult("storeID")
    }
    private val wearableConnectionRepository: WearableConnectionRepository = mock()
    private val siteStore: SiteStore = mock()
    private val appPrefs: AppPrefsWrapper = mock()
    private val dispatcher: FakeDispatcher = FakeDispatcher()

    private val siteObserver = SiteObserver(
        selectedSite = selectedSite,
        wooCommerceStore = wooCommerceStore,
        environmentRepository = environmentRepository,
        wearableConnectionRepository = wearableConnectionRepository,
        siteStore = siteStore,
        appPrefs = appPrefs,
        analyticsTracker = mock(),
        dispatcher = dispatcher
    )

    @Test
    fun `given app password connection, when starting observing, then fetch WPCom connect site info`() = testBlocking {
        val site = SiteModel().apply {
            url = "https://example.com"
            origin = SiteModel.ORIGIN_WPAPI
        }
        whenever(selectedSite.observe()).thenReturn(flowOf(site))
        whenever(siteStore.fetchConnectSiteInfoSync(site.url)).thenReturn(mock())

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
                    url = "https://example.com"
                    origin = SiteModel.ORIGIN_WPAPI
                }
                whenever(selectedSite.observe()).thenReturn(flowOf(site))
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
                url = "https://example.com"
                origin = SiteModel.ORIGIN_WPAPI
            }
            whenever(selectedSite.observe()).thenReturn(flowOf(site))
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
}
