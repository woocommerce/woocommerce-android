package org.wordpress.android.fluxc.store

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.settings.SiteSettingsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.settings.WPSettingsRestClient
import org.wordpress.android.fluxc.persistence.dao.WPSiteSettingsDao
import org.wordpress.android.fluxc.persistence.entity.WPSiteSettingsModel
import java.time.DayOfWeek

class WPSettingsStoreTest {
    private val restClient: WPSettingsRestClient = mock()
    private val dao = FakeWPSiteSettingsDao()
    private lateinit var store: WPSettingsStore

    @Before
    fun setUp() {
        store = WPSettingsStore(
            restClient = restClient,
            wpSiteSettingsDao = dao
        )
    }

    @Test
    fun `given monday response, when settings are fetched, then monday is cached`() = runTest {
        givenSuccess(startOfWeek = MONDAY)

        store.fetchSiteSettings(site)

        assertThat(store.getStartOfWeek(site)).isEqualTo(DayOfWeek.MONDAY)
    }

    @Test
    fun `given sunday response, when settings are fetched, then sunday is cached`() = runTest {
        givenSuccess(startOfWeek = SUNDAY)

        store.fetchSiteSettings(site)

        assertThat(store.getStartOfWeek(site)).isEqualTo(DayOfWeek.SUNDAY)
    }

    @Test
    fun `given invalid response, when settings are fetched, then unavailable value is cached`() = runTest {
        givenSuccess(startOfWeek = INVALID)

        store.fetchSiteSettings(site)

        assertThat(store.getStartOfWeek(site)).isNull()
        assertThat(store.getSiteSettingsAsync(site)?.startOfWeek).isNull()
    }

    @Test
    fun `given missing response, when settings are fetched, then unavailable value is cached`() = runTest {
        givenSuccess(startOfWeek = null)

        store.fetchSiteSettings(site)

        assertThat(store.getStartOfWeek(site)).isNull()
        assertThat(store.getSiteSettingsAsync(site)?.startOfWeek).isNull()
    }

    @Test
    fun `given network error and valid cache, when settings are fetched, then valid cache is preserved`() = runTest {
        dao.upsertSiteSettings(WPSiteSettingsModel(site.localId(), startOfWeek = DayOfWeek.MONDAY))
        givenError(GenericErrorType.NETWORK_ERROR)

        store.fetchSiteSettings(site)

        assertThat(store.getStartOfWeek(site)).isEqualTo(DayOfWeek.MONDAY)
    }

    @Test
    fun `given auth error and valid cache, when settings are fetched, then valid cache is preserved`() = runTest {
        dao.upsertSiteSettings(WPSiteSettingsModel(site.localId(), startOfWeek = DayOfWeek.MONDAY))
        givenError(GenericErrorType.HTTP_AUTH_ERROR)

        store.fetchSiteSettings(site)

        assertThat(store.getStartOfWeek(site)).isEqualTo(DayOfWeek.MONDAY)
    }

    private suspend fun givenSuccess(startOfWeek: Int?) {
        whenever(restClient.fetchSiteSettings(site))
            .thenReturn(WPAPIResponse.Success(SiteSettingsResponse(startOfWeek), emptyList()))
    }

    private suspend fun givenError(errorType: GenericErrorType) {
        whenever(restClient.fetchSiteSettings(site))
            .thenReturn(WPAPIResponse.Error(WPAPINetworkError(BaseNetworkError(errorType))))
    }

    private class FakeWPSiteSettingsDao : WPSiteSettingsDao() {
        private val settings = mutableMapOf<LocalId, WPSiteSettingsModel>()

        override suspend fun getSiteSettings(siteId: LocalId): WPSiteSettingsModel? = settings[siteId]

        override suspend fun upsertSiteSettings(model: WPSiteSettingsModel) {
            settings[model.localSiteId] = model
        }
    }

    private companion object {
        const val SUNDAY = 0
        const val MONDAY = 1
        const val INVALID = 7

        val site = SiteModel().apply {
            id = 1
            url = "https://example.com"
        }
    }
}
