package com.woocommerce.android.ui.login.auto

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.WPApiSiteRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@Suppress("FunctionNaming")
@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationPasswordAutoLoginStrategyTest : BaseUnitTest() {
    private val repository: WPApiSiteRepository = mock()
    private val selectedSite: SelectedSite = mock()
    private val discoveredSite = site()
    private val persistedSite = site().apply { username = USERNAME }
    private val request = request(AutoLoginConnection.WP_API)
    private val strategy = ApplicationPasswordAutoLoginStrategy(repository, selectedSite)

    @Before
    fun setUp() = testBlocking {
        whenever(repository.fetchSite(SITE_URL, null, null)).thenReturn(Result.success(discoveredSite))
        whenever(repository.getSiteByLocalId(SITE_LOCAL_ID)).thenReturn(persistedSite)
        whenever(repository.checkIfUserIsEligible(persistedSite)).thenReturn(Result.success(false))
    }

    @Test
    fun `when direct login succeeds, then authenticated user fetch happens before selection`() = testBlocking {
        val result = strategy.login(request)

        assertThat(result).isEqualTo(ApplicationPasswordAutoLoginStrategy.Result.Success)
        verify(repository).fetchSite(SITE_URL, null, null)
        verify(repository).saveApplicationPassword(SITE_LOCAL_ID, USERNAME, PASSWORD)
        inOrder(repository, selectedSite).run {
            verify(repository).checkIfUserIsEligible(persistedSite)
            verify(selectedSite).set(persistedSite)
        }
    }

    @Test
    fun `when authenticated user fetch fails, then login fails without selection`() = testBlocking {
        whenever(repository.checkIfUserIsEligible(persistedSite))
            .thenReturn(Result.failure(IllegalStateException("fixed failure")))

        val result = strategy.login(request)

        assertThat(result).isEqualTo(
            ApplicationPasswordAutoLoginStrategy.Result.Failure(AutoLoginStatus.AUTH_FAILED)
        )
        verify(selectedSite, never()).set(any())
    }

    companion object {
        const val SITE_URL = "https://store.example/shop"
        const val USERNAME = "merchant"
        const val PASSWORD = "application-password"
        private const val SITE_LOCAL_ID = 7

        internal fun request(connection: AutoLoginConnection) = AutoLoginRequest(
            connection = connection,
            siteUrl = SITE_URL,
            credentials = AutoLoginCredentials(USERNAME, PASSWORD)
        )

        internal fun site() = SiteModel().apply {
            id = SITE_LOCAL_ID
            siteId = SITE_REMOTE_ID
            url = SITE_URL
            origin = SiteModel.ORIGIN_WPAPI
        }

        private const val SITE_REMOTE_ID = 42L
    }
}
