package com.woocommerce.android.ui.jitm

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.jitm.clientside.ClientSideJitmBannerProvider
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMContent
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMCta

@ExperimentalCoroutinesApi
class JitmBannerMessageRepositoryTest : BaseUnitTest() {

    private val selectedSite: SelectedSite = mock()
    private val jitmBannerAdapter: JitmBannerAdapter = mock()
    private val clientSideBannerProvider: ClientSideJitmBannerProvider = mock()

    private lateinit var sut: JitmBannerMessageRepository

    @Before
    fun setup() {
        sut = JitmBannerMessageRepository(
            selectedSite = selectedSite,
            jitmBannerAdapter = jitmBannerAdapter,
            clientSideBannerProvider = clientSideBannerProvider,
        )
    }

    @Test
    fun `given jetpack connected site, when getMessagesForPath, then uses jitm adapter`() =
        testBlocking {
            val site = createSite(isJetpackConnected = true)
            whenever(selectedSite.getIfExists()).thenReturn(site)
            whenever(jitmBannerAdapter.getMessagesForPath(any())).thenReturn(listOf(createJitmResponse()))

            val result = sut.getMessagesForPath(JitmMessagePathsProvider.MY_STORE)

            assertThat(result).hasSize(1)
            verify(jitmBannerAdapter).getMessagesForPath(JitmMessagePathsProvider.MY_STORE)
            verify(clientSideBannerProvider, never()).getMessagesForPath(any())
        }

    @Test
    fun `given non-jetpack site, when getMessagesForPath, then uses client side provider`() =
        testBlocking {
            val site = createSite(isJetpackConnected = false)
            whenever(selectedSite.getIfExists()).thenReturn(site)
            whenever(clientSideBannerProvider.getMessagesForPath(any())).thenReturn(listOf(createJitmResponse()))

            val result = sut.getMessagesForPath(JitmMessagePathsProvider.MY_STORE)

            assertThat(result).hasSize(1)
            verify(clientSideBannerProvider).getMessagesForPath(JitmMessagePathsProvider.MY_STORE)
            verify(jitmBannerAdapter, never()).getMessagesForPath(any())
        }

    @Test
    fun `given no site, when getMessagesForPath, then uses client side provider`() =
        testBlocking {
            whenever(selectedSite.getIfExists()).thenReturn(null)
            whenever(clientSideBannerProvider.getMessagesForPath(any())).thenReturn(emptyList())

            val result = sut.getMessagesForPath(JitmMessagePathsProvider.MY_STORE)

            assertThat(result).isEmpty()
            verify(clientSideBannerProvider).getMessagesForPath(JitmMessagePathsProvider.MY_STORE)
            verify(jitmBannerAdapter, never()).getMessagesForPath(any())
        }

    @Test
    fun `given jetpack connected site, when dismissMessage, then uses jitm adapter`() =
        testBlocking {
            val site = createSite(isJetpackConnected = true)
            whenever(selectedSite.getIfExists()).thenReturn(site)
            whenever(jitmBannerAdapter.dismissMessage(any(), any(), any())).thenReturn(true)

            val result = sut.dismissMessage(JitmMessagePathsProvider.MY_STORE, "id", "feature")

            assertThat(result).isTrue()
            verify(jitmBannerAdapter).dismissMessage(JitmMessagePathsProvider.MY_STORE, "id", "feature")
            verify(clientSideBannerProvider, never()).dismissMessage(any(), any(), any())
        }

    @Test
    fun `given non-jetpack site, when dismissMessage, then uses client side provider`() =
        testBlocking {
            val site = createSite(isJetpackConnected = false)
            whenever(selectedSite.getIfExists()).thenReturn(site)
            whenever(clientSideBannerProvider.dismissMessage(any(), any(), any())).thenReturn(true)

            val result = sut.dismissMessage(JitmMessagePathsProvider.MY_STORE, "id", "feature")

            assertThat(result).isTrue()
            verify(clientSideBannerProvider).dismissMessage(JitmMessagePathsProvider.MY_STORE, "id", "feature")
            verify(jitmBannerAdapter, never()).dismissMessage(any(), any(), any())
        }

    @Test
    fun `given jetpack connected site, when onCtaClicked, then uses jitm adapter`() {
        val site = createSite(isJetpackConnected = true)
        whenever(selectedSite.getIfExists()).thenReturn(site)

        sut.onCtaClicked(JitmMessagePathsProvider.MY_STORE, "jitm_id")

        verify(jitmBannerAdapter).onCtaClicked(JitmMessagePathsProvider.MY_STORE, "jitm_id")
        verify(clientSideBannerProvider, never()).onCtaClicked(any(), any())
    }

    @Test
    fun `given non-jetpack site, when onCtaClicked, then uses client side provider`() {
        val site = createSite(isJetpackConnected = false)
        whenever(selectedSite.getIfExists()).thenReturn(site)

        sut.onCtaClicked(JitmMessagePathsProvider.MY_STORE, "banner_id")

        verify(clientSideBannerProvider).onCtaClicked(JitmMessagePathsProvider.MY_STORE, "banner_id")
        verify(jitmBannerAdapter, never()).onCtaClicked(any(), any())
    }

    private fun createSite(isJetpackConnected: Boolean): SiteModel {
        return SiteModel().apply {
            if (isJetpackConnected) {
                origin = SiteModel.ORIGIN_WPCOM_REST
                setIsJetpackConnected(true)
            }
        }
    }

    private fun createJitmResponse() = JITMApiResponse(
        template = "banner",
        content = JITMContent(
            message = "message",
            description = "description",
            icon = "",
            iconPath = null,
            title = ""
        ),
        cta = JITMCta(
            message = "cta",
            link = "https://example.com"
        ),
        timeToLive = 0,
        id = "id",
        featureClass = "feature",
        expires = 0L,
        maxDismissal = 1,
        isDismissible = true,
        url = "",
        jitmStatsUrl = "",
        assets = null
    )
}
