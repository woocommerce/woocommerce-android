package com.woocommerce.android.ui.jitm.clientside

import com.woocommerce.android.ui.jitm.JitmMessagePathsProvider
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMContent
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMCta

@ExperimentalCoroutinesApi
class ClientSideJitmBannerProviderTest : BaseUnitTest() {

    private val banner1: ClientSideBanner = mock()
    private val banner2: ClientSideBanner = mock()

    private lateinit var sut: ClientSideJitmBannerProvider

    @Before
    fun setup() {
        sut = ClientSideJitmBannerProvider(setOf(banner1, banner2))
    }

    @Test
    fun `given matching banner that should show, when getMessagesForPath, then returns banner response`() =
        testBlocking {
            whenever(banner1.messagePath).thenReturn(JitmMessagePathsProvider.MY_STORE)
            whenever(banner1.shouldShow()).thenReturn(true)
            whenever(banner1.toJitmResponse()).thenReturn(createJitmResponse("banner1"))
            whenever(banner2.messagePath).thenReturn("other_path")

            val result = sut.getMessagesForPath(JitmMessagePathsProvider.MY_STORE)

            assertThat(result).hasSize(1)
            assertThat(result.first().id).isEqualTo("banner1")
        }

    @Test
    fun `given matching banner that should not show, when getMessagesForPath, then returns empty list`() =
        testBlocking {
            whenever(banner1.messagePath).thenReturn(JitmMessagePathsProvider.MY_STORE)
            whenever(banner1.shouldShow()).thenReturn(false)
            whenever(banner2.messagePath).thenReturn("other_path")

            val result = sut.getMessagesForPath(JitmMessagePathsProvider.MY_STORE)

            assertThat(result).isEmpty()
        }

    @Test
    fun `given no matching banners, when getMessagesForPath, then returns empty list`() =
        testBlocking {
            whenever(banner1.messagePath).thenReturn("other_path1")
            whenever(banner2.messagePath).thenReturn("other_path2")

            val result = sut.getMessagesForPath(JitmMessagePathsProvider.MY_STORE)

            assertThat(result).isEmpty()
        }

    @Test
    fun `given multiple matching banners, when getMessagesForPath, then returns all matching banners`() =
        testBlocking {
            whenever(banner1.messagePath).thenReturn(JitmMessagePathsProvider.MY_STORE)
            whenever(banner1.shouldShow()).thenReturn(true)
            whenever(banner1.toJitmResponse()).thenReturn(createJitmResponse("banner1"))
            whenever(banner2.messagePath).thenReturn(JitmMessagePathsProvider.MY_STORE)
            whenever(banner2.shouldShow()).thenReturn(true)
            whenever(banner2.toJitmResponse()).thenReturn(createJitmResponse("banner2"))

            val result = sut.getMessagesForPath(JitmMessagePathsProvider.MY_STORE)

            assertThat(result).hasSize(2)
        }

    @Test
    fun `when dismissMessage called, then matching banner is dismissed`() =
        testBlocking {
            whenever(banner1.bannerId).thenReturn("banner1_id")

            val result = sut.dismissMessage(JitmMessagePathsProvider.MY_STORE, "banner1_id", "feature")

            assertThat(result).isTrue()
            verify(banner1).onDismiss()
            verify(banner2, never()).onDismiss()
        }

    @Test
    fun `when dismissMessage called with non-matching id, then no banner is dismissed`() =
        testBlocking {
            whenever(banner1.bannerId).thenReturn("banner1_id")
            whenever(banner2.bannerId).thenReturn("banner2_id")

            val result = sut.dismissMessage(JitmMessagePathsProvider.MY_STORE, "unknown_id", "feature")

            assertThat(result).isTrue()
            verify(banner1, never()).onDismiss()
            verify(banner2, never()).onDismiss()
        }

    @Test
    fun `when onCtaClicked called, then matching banner receives click`() {
        whenever(banner1.bannerId).thenReturn("banner1_id")

        sut.onCtaClicked(JitmMessagePathsProvider.MY_STORE, "banner1_id")

        verify(banner1).onCtaClick()
        verify(banner2, never()).onCtaClick()
    }

    private fun createJitmResponse(id: String) = JITMApiResponse(
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
        id = id,
        featureClass = "feature",
        expires = 0L,
        maxDismissal = 1,
        isDismissible = true,
        url = "",
        jitmStatsUrl = "",
        assets = null
    )
}
