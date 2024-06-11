package com.woocommerce.android.ui.jitm

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.jitm.JitmViewModel.Companion.JITM_MESSAGE_PATH_KEY
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMContent
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMCta

@ExperimentalCoroutinesApi
class JitmViewModelTest : BaseUnitTest() {
    private val savedState: SavedStateHandle = mock {
        on { get<String>(JITM_MESSAGE_PATH_KEY) }.thenReturn("woomobile:my_store:admin_notices")
    }
    private val jitmStoreInMemoryCache: JitmStoreInMemoryCache = mock()
    private val jitmTracker: JitmTracker = mock()
    private val utmProvider: JitmUtmProvider = mock()
    private val selectedSite: SelectedSite = mock()

    private lateinit var sut: JitmViewModel

    @Test
    fun `given jitm success response, when viewmodel init, then proper banner state event is triggered`() {
        testBlocking {
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse()
                )
            )

            whenViewModelIsCreated()

            assertThat(sut.jitmState.value).isInstanceOf(JitmState.Banner::class.java)
        }
    }

    @Test
    fun `given jitm empty response, when viewmodel init, then banner state hide event is triggered`() {
        testBlocking {
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                emptyList()
            )

            whenViewModelIsCreated()

            assertThat(sut.jitmState.value).isInstanceOf(JitmState.Hidden::class.java)
        }
    }

    @Test
    fun `given jitm success response, when viewmodel init, then proper jitm message is used in UI`() {
        testBlocking {
            val testJitmMessage = "Test jitm message"

            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse(
                        content = provideJitmContent(message = testJitmMessage)
                    )
                )
            )

            whenViewModelIsCreated()

            assertThat(
                (sut.jitmState.value as JitmState.Banner).title
            ).isEqualTo(
                UiString.UiStringText(text = testJitmMessage, containsHtml = false)
            )
        }
    }

    @Test
    fun `given jitm success response with background image, when viewmodel init, then proper jitm background image is used in UI`() {
        testBlocking {
            val imageUrl = "https://test.com/image.png"
            val imageDarkUrl = "https://test.com/image_dark.png"
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse(
                        content = provideJitmContent(),
                        assets = mapOf(
                            "background_image_url" to imageUrl,
                            "background_image_dark_url" to imageDarkUrl
                        )
                    )
                )
            )

            whenViewModelIsCreated()

            assertThat(
                (sut.jitmState.value as JitmState.Banner).backgroundImage
            ).isEqualTo(
                JitmState.Banner.LocalOrRemoteImage.Remote(imageUrl, imageDarkUrl)
            )
            assertThat(
                (sut.jitmState.value as JitmState.Banner).badgeIcon
            ).isEqualTo(
                JitmState.Banner.LabelOrRemoteIcon.Label(
                    UiString.UiStringRes(R.string.card_reader_upsell_card_reader_banner_new)
                )
            )
        }
    }

    @Test
    fun `given jitm success response with only light background image, when viewmodel init, then proper jitm light background image is used in UI`() {
        testBlocking {
            val imageUrl = "https://test.com/image.png"
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse(
                        content = provideJitmContent(),
                        assets = mapOf("background_image_url" to imageUrl)
                    )
                )
            )

            whenViewModelIsCreated()

            assertThat(
                (sut.jitmState.value as JitmState.Banner).backgroundImage
            ).isEqualTo(
                JitmState.Banner.LocalOrRemoteImage.Remote(imageUrl, imageUrl)
            )
            assertThat(
                (sut.jitmState.value as JitmState.Banner).badgeIcon
            ).isEqualTo(
                JitmState.Banner.LabelOrRemoteIcon.Label(
                    UiString.UiStringRes(R.string.card_reader_upsell_card_reader_banner_new)
                )
            )
        }
    }

    @Test
    fun `given jitm success response with badge image, when viewmodel init, then proper jitm badge icon is used in UI`() {
        testBlocking {
            val imageUrl = "https://test.com/image.png"
            val imageDarkUrl = "https://test.com/image_dark.png"
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse(
                        content = provideJitmContent(),
                        assets = mapOf(
                            "badge_image_url" to imageUrl,
                            "badge_image_dark_url" to imageDarkUrl
                        )
                    )
                )
            )

            whenViewModelIsCreated()

            assertThat(
                (sut.jitmState.value as JitmState.Banner).badgeIcon
            ).isEqualTo(
                JitmState.Banner.LabelOrRemoteIcon.Remote(imageUrl, imageDarkUrl)
            )
            assertThat(
                (sut.jitmState.value as JitmState.Banner).backgroundImage
            ).isEqualTo(
                JitmState.Banner.LocalOrRemoteImage.Local(R.drawable.ic_banner_upsell_card_reader_illustration)
            )
        }
    }

    @Test
    fun `given jitm success response with only light badge image, when viewmodel init, then light jitm badge icon is used in UI`() {
        testBlocking {
            val imageUrl = "https://test.com/image.png"
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse(
                        content = provideJitmContent(),
                        assets = mapOf("badge_image_url" to imageUrl)
                    )
                )
            )

            whenViewModelIsCreated()

            assertThat(
                (sut.jitmState.value as JitmState.Banner).badgeIcon
            ).isEqualTo(
                JitmState.Banner.LabelOrRemoteIcon.Remote(imageUrl, imageUrl)
            )
            assertThat(
                (sut.jitmState.value as JitmState.Banner).backgroundImage
            ).isEqualTo(
                JitmState.Banner.LocalOrRemoteImage.Local(R.drawable.ic_banner_upsell_card_reader_illustration)
            )
        }
    }

    @Test
    fun `given jitm success response, when viewmodel init, then proper jitm description is used in UI`() {
        testBlocking {
            val testJitmDescription = "Test jitm description"
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse(
                        content = provideJitmContent(description = testJitmDescription)
                    )
                )
            )

            whenViewModelIsCreated()

            assertThat(
                (sut.jitmState.value as JitmState.Banner).description
            ).isEqualTo(
                UiString.UiStringText(text = testJitmDescription, containsHtml = false)
            )
        }
    }

    @Test
    fun `given jitm success response with modal, when viewmodel init, then proper jitm is used in UI`() {
        testBlocking {
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse(
                        template = "modal",
                        jitmCta = provideJitmCta(message = "primaryActionLabel"),
                        content = provideJitmContent(
                            message = "message",
                            description = "description",
                        ),
                        assets = mapOf(
                            "background_image_url" to "imageLightUrl",
                            "background_image_dark_url" to "imageDarkUrl"
                        )
                    )
                )
            )

            whenViewModelIsCreated()

            val modal = sut.jitmState.value as JitmState.Modal
            assertThat(modal.title).isEqualTo(UiString.UiStringText("message"))
            assertThat(modal.description)
                .isEqualTo(UiString.UiStringText("description"))
            assertThat(modal.backgroundLightImageUrl).isEqualTo("imageLightUrl")
            assertThat(modal.backgroundDarkImageUrl).isEqualTo("imageDarkUrl")
            assertThat(modal.primaryActionLabel).isEqualTo(UiString.UiStringText("primaryActionLabel"))
        }
    }

    @Test
    fun `given jitm success response with modal and light image url, when viewmodel init, then light image jitm is used in UI`() {
        testBlocking {
            val imageUrl = "https://test.com/image.png"
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse(
                        template = "modal",
                        jitmCta = provideJitmCta(message = "primaryActionLabel"),
                        content = provideJitmContent(
                            message = "message",
                            description = "description",
                        ),
                        assets = mapOf("background_image_url" to imageUrl)
                    )
                )
            )

            whenViewModelIsCreated()

            val modal = sut.jitmState.value as JitmState.Modal
            assertThat(modal.title).isEqualTo(UiString.UiStringText("message"))
            assertThat(modal.description)
                .isEqualTo(UiString.UiStringText("description"))
            assertThat(modal.backgroundLightImageUrl).isEqualTo(imageUrl)
            assertThat(modal.backgroundDarkImageUrl).isEqualTo(imageUrl)
            assertThat(modal.primaryActionLabel).isEqualTo(UiString.UiStringText("primaryActionLabel"))
        }
    }

    @Test
    fun `given jitm success response with not modal or banner, when viewmodel init, then banner jitm is used in UI`() {
        testBlocking {
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse(
                        template = "not_modal_or_banner",
                    )
                )
            )

            whenViewModelIsCreated()
            assertThat(sut.jitmState.value).isInstanceOf(JitmState.Banner::class.java)
        }
    }

    @Test
    fun `given jitm success response with banner, when viewmodel init, then banner jitm is used in UI`() {
        testBlocking {
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse(
                        template = "banner",
                    )
                )
            )

            whenViewModelIsCreated()
            assertThat(sut.jitmState.value).isInstanceOf(JitmState.Banner::class.java)
        }
    }

    @Test
    fun `given jitm success response, when viewmodel init, then proper jitm cta label is used in UI`() {
        testBlocking {
            val testJitmCtaLabel = "Test jitm Cta label"
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse(
                        jitmCta = provideJitmCta(message = testJitmCtaLabel)
                    )
                )
            )

            whenViewModelIsCreated()

            assertThat(
                (sut.jitmState.value as JitmState.Banner).primaryActionLabel
            ).isEqualTo(
                UiString.UiStringText(text = testJitmCtaLabel, containsHtml = false)
            )
        }
    }

    @Test
    fun `given jitm displayed, when jitm cta clicked, then jitm click event emitted`() {
        whenever(selectedSite.getIfExists()).thenReturn(SiteModel())
        testBlocking {
            val jitmCtaLink = "https://woocommerce.com/products/hardware/US"
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse(
                        jitmCta = provideJitmCta(
                            link = jitmCtaLink
                        )
                    )
                )
            )
            whenever(
                utmProvider.getUrlWithUtmParams(
                    anyString(),
                    anyString(),
                    anyString(),
                    any(),
                    anyString(),
                )
            ).thenReturn(jitmCtaLink)

            whenViewModelIsCreated()
            (sut.jitmState.value as JitmState.Banner).onPrimaryActionClicked.invoke()

            assertThat(sut.event.value).isInstanceOf(JitmViewModel.CtaClick::class.java)
        }
    }

    @Test
    fun `given jitm displayed, when jitm cta clicked, then jitm click called from cache`() {
        whenever(selectedSite.getIfExists()).thenReturn(SiteModel())
        testBlocking {
            val jitmCtaLink = "https://woocommerce.com/products/hardware/US"
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse(
                        jitmCta = provideJitmCta(
                            link = jitmCtaLink
                        )
                    )
                )
            )
            whenever(
                utmProvider.getUrlWithUtmParams(
                    anyString(),
                    anyString(),
                    anyString(),
                    any(),
                    anyString(),
                )
            ).thenReturn(jitmCtaLink)

            whenViewModelIsCreated()
            (sut.jitmState.value as JitmState.Banner).onPrimaryActionClicked.invoke()

            verify(jitmStoreInMemoryCache).onCtaClicked("woomobile:my_store:admin_notices")
        }
    }

    @Test
    fun `given jitm displayed, when jitm cta clicked, then proper url is passedto OpenJITM event`() {
        val jitmCtaLink = "https://woocommerce.com/products/hardware/US"
        val jitmCtaLinkWithUtmParams = "https://woocommerce.com/products/hardware/US?utm_campaign=compaign"
        whenever(selectedSite.getIfExists()).thenReturn(SiteModel())
        testBlocking {
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse(
                        jitmCta = provideJitmCta(
                            link = jitmCtaLink
                        )
                    )
                )
            )
            whenever(
                utmProvider.getUrlWithUtmParams(
                    anyString(),
                    anyString(),
                    anyString(),
                    any(),
                    eq(jitmCtaLink),
                )
            ).thenReturn(
                jitmCtaLinkWithUtmParams
            )

            whenViewModelIsCreated()
            (sut.jitmState.value as JitmState.Banner).onPrimaryActionClicked.invoke()

            assertThat(sut.event.value as JitmViewModel.CtaClick).isEqualTo(
                JitmViewModel.CtaClick(jitmCtaLinkWithUtmParams)
            )
        }
    }

    @Test
    fun `when fetch jitms, then fetch JITMS twice`() {
        testBlocking {
            whenViewModelIsCreated()

            sut.fetchJitms()

            // called twice, on view model init and on pull to refresh
            verify(jitmStoreInMemoryCache, times(2)).getMessagesForPath(any())
        }
    }

    @Test
    fun `given store setup in US, when viewmodel init, then request for jitm with valid message path`() {
        testBlocking {
            val expectedMessagePath = "woomobile:my_store:admin_notices"
            val captor = argumentCaptor<String>()

            whenViewModelIsCreated()
            verify(jitmStoreInMemoryCache).getMessagesForPath(captor.capture())

            assertThat(captor.firstValue).isEqualTo(expectedMessagePath)
        }
    }

    @Test
    fun `given jitm displayed, when jitm dismiss tapped, then banner state is updated to not display`() {
        testBlocking {
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse()
                )
            )

            whenViewModelIsCreated()
            (sut.jitmState.value as JitmState.Banner).onDismissClicked.invoke()

            assertThat(sut.jitmState.value).isInstanceOf(JitmState.Hidden::class.java)
        }
    }

    @Test
    fun `given jitm success response, when viewmodel init, then jitm displayed is tracked`() {
        testBlocking {
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse()
                )
            )

            whenViewModelIsCreated()

            verify(jitmTracker).trackJitmDisplayed(
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `given jitm success, when viewmodel init, then jitm displayed is tracked with correct properties`() {
        testBlocking {
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse(
                        id = "12345",
                        featureClass = "woomobile_ipp"
                    )
                )
            )

            whenViewModelIsCreated()

            verify(jitmTracker).trackJitmDisplayed(
                UTM_SOURCE,
                "12345",
                "woomobile_ipp"
            )
        }
    }

    @Test
    fun `given jitm displayed, when cta tapped, then cta tapped event is tracked`() {
        whenever(selectedSite.getIfExists()).thenReturn(SiteModel())
        testBlocking {
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse()
                )
            )
            whenever(
                utmProvider.getUrlWithUtmParams(
                    anyString(),
                    anyString(),
                    anyString(),
                    any(),
                    anyString(),
                )
            ).thenReturn("")

            whenViewModelIsCreated()
            (sut.jitmState.value as JitmState.Banner).onPrimaryActionClicked.invoke()

            verify(jitmTracker).trackJitmCtaTapped(
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `given jitm displayed, when cta tapped, then cta tapped event is tracked with correct properties`() {
        testBlocking {
            whenever(selectedSite.getIfExists()).thenReturn(SiteModel())
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse(
                        id = "12345",
                        featureClass = "woomobile_ipp"
                    )
                )
            )
            whenever(
                utmProvider.getUrlWithUtmParams(
                    anyString(),
                    anyString(),
                    anyString(),
                    any(),
                    anyString(),
                )
            ).thenReturn("")

            whenViewModelIsCreated()
            (sut.jitmState.value as JitmState.Banner).onPrimaryActionClicked.invoke()

            verify(jitmTracker).trackJitmCtaTapped(
                UTM_SOURCE,
                "12345",
                "woomobile_ipp"
            )
        }
    }

    @Test
    fun `given jitm displayed, when dismiss tapped, then dismiss tapped event is tracked`() {
        testBlocking {
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse()
                )
            )

            whenViewModelIsCreated()
            (sut.jitmState.value as JitmState.Banner).onDismissClicked.invoke()

            verify(jitmTracker).trackJitmDismissTapped(
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `given jitm displayed, when dismiss tapped, then dismiss tapped event is tracked with correct properties`() {
        testBlocking {
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse(
                        id = "12345",
                        featureClass = "woomobile_ipp"
                    )
                )
            )

            whenViewModelIsCreated()
            (sut.jitmState.value as JitmState.Banner).onDismissClicked.invoke()

            verify(jitmTracker).trackJitmDismissTapped(
                UTM_SOURCE,
                "12345",
                "woomobile_ipp"
            )
        }
    }

    @Test
    fun `given jitm dismissed, when dismiss success, then dismiss success event is tracked`() {
        testBlocking {
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse()
                )
            )
            whenever(
                jitmStoreInMemoryCache.dismissJitmMessage(any(), any(), any())
            ).thenReturn(
                WooResult(true)
            )

            whenViewModelIsCreated()
            (sut.jitmState.value as JitmState.Banner).onDismissClicked.invoke()

            verify(jitmTracker).trackJitmDismissSuccess(
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `given jitm dismissed, when dismiss success, then dismiss success event is tracked with correct properties`() {
        testBlocking {
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse(
                        id = "12345",
                        featureClass = "woomobile_ipp"
                    )
                )
            )
            whenever(
                jitmStoreInMemoryCache.dismissJitmMessage(any(), any(), any())
            ).thenReturn(
                WooResult(true)
            )

            whenViewModelIsCreated()
            (sut.jitmState.value as JitmState.Banner).onDismissClicked.invoke()

            verify(jitmTracker).trackJitmDismissSuccess(
                UTM_SOURCE,
                "12345",
                "woomobile_ipp"
            )
        }
    }

    @Test
    fun `given jitm dismissed, when dismiss failure, then dismiss failure event is tracked`() {
        testBlocking {
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse()
                )
            )
            whenever(
                jitmStoreInMemoryCache.dismissJitmMessage(any(), any(), any())
            ).thenReturn(
                WooResult(false)
            )

            whenViewModelIsCreated()
            (sut.jitmState.value as JitmState.Banner).onDismissClicked.invoke()

            verify(jitmTracker).trackJitmDismissFailure(
                anyString(),
                anyString(),
                anyString(),
                eq(null),
                eq(null)
            )
        }
    }

    @Test
    fun `given jitm dismissed, when dismiss error, then dismiss failure event is tracked`() {
        testBlocking {
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse()
                )
            )
            whenever(
                jitmStoreInMemoryCache.dismissJitmMessage(any(), any(), any())
            ).thenReturn(
                WooResult(
                    WooError(
                        type = WooErrorType.GENERIC_ERROR,
                        original = BaseRequest.GenericErrorType.NETWORK_ERROR
                    )
                )
            )

            whenViewModelIsCreated()
            (sut.jitmState.value as JitmState.Banner).onDismissClicked.invoke()

            verify(jitmTracker).trackJitmDismissFailure(
                anyString(),
                anyString(),
                anyString(),
                any(),
                eq(null)
            )
        }
    }

    @Test
    fun `given jitm dismissed, when dismiss error, then dismiss failure event is tracked with correct properties`() {
        testBlocking {
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse(
                        id = "12345",
                        featureClass = "woomobile_ipp"
                    )
                )
            )
            whenever(
                jitmStoreInMemoryCache.dismissJitmMessage(any(), any(), any())
            ).thenReturn(
                WooResult(
                    WooError(
                        type = WooErrorType.GENERIC_ERROR,
                        original = BaseRequest.GenericErrorType.NETWORK_ERROR,
                        message = "Generic error"
                    )
                )
            )

            whenViewModelIsCreated()
            (sut.jitmState.value as JitmState.Banner).onDismissClicked.invoke()

            verify(jitmTracker).trackJitmDismissFailure(
                UTM_SOURCE,
                "12345",
                "woomobile_ipp",
                WooErrorType.GENERIC_ERROR,
                "Generic error"
            )
        }
    }

    @Test
    fun `given jitm dismissed, when dismiss failure, then dismiss failure event is tracked with correct properties`() {
        testBlocking {
            whenever(
                jitmStoreInMemoryCache.getMessagesForPath(any())
            ).thenReturn(
                listOf(
                    provideJitmApiResponse(
                        id = "12345",
                        featureClass = "woomobile_ipp"
                    )
                )
            )
            whenever(
                jitmStoreInMemoryCache.dismissJitmMessage(any(), eq("12345"), eq("woomobile_ipp"))
            ).thenReturn(
                WooResult(false)
            )

            whenViewModelIsCreated()
            (sut.jitmState.value as JitmState.Banner).onDismissClicked.invoke()

            verify(jitmTracker).trackJitmDismissFailure(
                UTM_SOURCE,
                "12345",
                "woomobile_ipp",
                null,
                null
            )
        }
    }

    private fun whenViewModelIsCreated() {
        sut = JitmViewModel(
            savedState,
            jitmStoreInMemoryCache,
            jitmTracker,
            utmProvider,
            selectedSite,
        )
    }

    private fun provideJitmApiResponse(
        template: String = "",
        content: JITMContent = provideJitmContent(),
        jitmCta: JITMCta = provideJitmCta(),
        timeToLive: Int = 0,
        id: String = "",
        featureClass: String = "",
        expires: Long = 0L,
        maxDismissal: Int = 2,
        isDismissible: Boolean = false,
        url: String = "",
        jitmStatsUrl: String = "",
        assets: Map<String, String>? = null,
    ) = JITMApiResponse(
        template = template,
        content = content,
        cta = jitmCta,
        timeToLive = timeToLive,
        id = id,
        featureClass = featureClass,
        expires = expires,
        maxDismissal = maxDismissal,
        isDismissible = isDismissible,
        url = url,
        jitmStatsUrl = jitmStatsUrl,
        assets = assets
    )

    private fun provideJitmContent(
        message: String = "",
        description: String = "",
        icon: String = "",
        iconPath: String = "",
        title: String = ""
    ) = JITMContent(
        message = message,
        description = description,
        icon = icon,
        iconPath = iconPath,
        title = title,
    )

    private fun provideJitmCta(
        message: String = "",
        link: String = ""
    ) = JITMCta(
        message = message,
        link = link
    )

    private companion object {
        val WOO_GENERIC_ERROR = WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN)
        const val UTM_SOURCE = "my_store"
    }
}
