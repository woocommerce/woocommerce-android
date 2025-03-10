package com.woocommerce.android.ui.login.accountmismatch

import com.woocommerce.android.FakeDispatcher
import com.woocommerce.android.ui.login.WPApiSiteRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.jetpack.JetpackConnectionData
import org.wordpress.android.fluxc.model.jetpack.JetpackUser
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.SiteStore

@OptIn(ExperimentalCoroutinesApi::class)
class AccountMismatchRepositoryTest : BaseUnitTest() {
    private val jetpackStore = mock<JetpackStore>()
    private val siteStore = mock<SiteStore>()
    private val wpApiSiteRepository = mock<WPApiSiteRepository> {
        onBlocking { fetchSite(any(), any(), any()) }.thenReturn(Result.success(SiteModel()))
    }
    private val dispatcher = FakeDispatcher()

    private val sut = AccountMismatchRepository(
        jetpackStore = jetpackStore,
        siteStore = siteStore,
        wpApiSiteRepository = wpApiSiteRepository,
        dispatcher = dispatcher
    )

    @Test
    fun `given a non-connected Jetpack Account, when fetching status, then return correct status`() = testBlocking {
        whenever(jetpackStore.fetchJetpackConnectionData(any(), eq(false)))
            .thenReturn(JetpackStore.JetpackResult(createJetpackConnectionData(isConnected = false)))

        val result = sut.checkJetpackConnection(
            siteUrl = "https://example.com",
            username = "username",
            password = "password"
        )

        assertThat(result.getOrNull()).isEqualTo(AccountMismatchRepository.JetpackConnectionStatus.NotConnected)
    }

    @Test
    fun `given a null jetpack user, when fetching connection status, then assume non-connected`() = testBlocking {
        whenever(jetpackStore.fetchJetpackConnectionData(any(), eq(false)))
            .thenReturn(JetpackStore.JetpackResult(null))

        val result = sut.checkJetpackConnection(
            siteUrl = "https://example.com",
            username = "username",
            password = "password"
        )

        assertThat(result.getOrNull()).isEqualTo(AccountMismatchRepository.JetpackConnectionStatus.NotConnected)
    }

    @Test
    fun `given a connected Jetpack Account, when fetching status, then return correct status`() = testBlocking {
        whenever(jetpackStore.fetchJetpackConnectionData(any(), eq(false)))
            .thenReturn(JetpackStore.JetpackResult(createJetpackConnectionData(isConnected = true, wpcomEmail = "email")))

        val result = sut.checkJetpackConnection(
            siteUrl = "https://example.com",
            username = "username",
            password = "password"
        )

        assertThat(result.getOrNull())
            .isEqualTo(AccountMismatchRepository.JetpackConnectionStatus.ConnectedToDifferentAccount("email"))
    }

    @Test
    fun `given a correctly connected Jetpack account, when fetching email, then return it`() = testBlocking {
        whenever(jetpackStore.fetchJetpackConnectionData(any(), eq(false)))
            .thenReturn(JetpackStore.JetpackResult(createJetpackConnectionData(isConnected = true, wpcomEmail = "email")))

        val result = sut.fetchJetpackConnectedEmail(SiteModel())

        assertThat(result.getOrNull()).isEqualTo("email")
    }

    @Test
    fun `given an issue with Jetpack account, when fetching email, then return error`() = testBlocking {
        whenever(jetpackStore.fetchJetpackConnectionData(any(), eq(false)))
            .thenReturn(JetpackStore.JetpackResult(createJetpackConnectionData(isConnected = true, wpcomEmail = "")))

        val result = sut.fetchJetpackConnectedEmail(SiteModel())

        assertThat(result.isFailure).isTrue()
    }

    private fun createJetpackConnectionData(
        isConnected: Boolean = false,
        wpcomEmail: String = ""
    ) = JetpackConnectionData(
        currentUser = JetpackUser(
            isConnected = isConnected,
            wpcomEmail = wpcomEmail,
            isMaster = false,
            username = "username",
            wpcomId = 1L,
            wpcomUsername = "wpcomUsername"
        ),
        isSiteRegistered = null,
        blogId = 1L,
        connectionOwner = null
    )
}
