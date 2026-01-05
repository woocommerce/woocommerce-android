package com.woocommerce.android.ui.jitm.clientside

import android.content.Context
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.jitm.JitmMessagePathsProvider
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.woopos.WooPosIsScreenSizeAllowed
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class ClientSidePosBannerTest : BaseUnitTest() {

    private val context: Context = mock {
        on { getString(any()) }.thenReturn("Test String")
    }
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val selectedSite: SelectedSite = mock()
    private val wooStore: WooCommerceStore = mock()
    private val wooPosIsScreenSizeAllowed: WooPosIsScreenSizeAllowed = mock()
    private val dismissalStorage: ClientSideBannerDismissalStorage = mock()

    private lateinit var sut: ClientSidePosBanner

    @Before
    fun setup() {
        sut = ClientSidePosBanner(
            context = context,
            appPrefsWrapper = appPrefsWrapper,
            selectedSite = selectedSite,
            wooStore = wooStore,
            wooPosIsScreenSizeAllowed = wooPosIsScreenSizeAllowed,
            dismissalStorage = dismissalStorage,
        )
    }

    @Test
    fun `given no site selected, when shouldShow called, then returns false`() {
        whenever(selectedSite.getIfExists()).thenReturn(null)

        val result = sut.shouldShow()

        assertThat(result).isFalse()
    }

    @Test
    fun `given tablet device, when shouldShow called, then returns false`() {
        setupValidSite()
        whenever(wooPosIsScreenSizeAllowed()).thenReturn(true)

        val result = sut.shouldShow()

        assertThat(result).isFalse()
    }

    @Test
    fun `given non-eligible country, when shouldShow called, then returns false`() {
        val site = setupValidSite()
        whenever(wooPosIsScreenSizeAllowed()).thenReturn(false)
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("CA")

        val result = sut.shouldShow()

        assertThat(result).isFalse()
    }

    @Test
    fun `given user has used IPP, when shouldShow called, then returns false`() {
        val site = setupValidSite()
        whenever(wooPosIsScreenSizeAllowed()).thenReturn(false)
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("US")
        whenever(
            appPrefsWrapper.getCardReaderPreferredPlugin(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId
            )
        ).thenReturn(PluginType.WOOCOMMERCE_PAYMENTS)

        val result = sut.shouldShow()

        assertThat(result).isFalse()
    }

    @Test
    fun `given banner already dismissed, when shouldShow called, then returns false`() {
        val site = setupValidSite()
        whenever(wooPosIsScreenSizeAllowed()).thenReturn(false)
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("US")
        whenever(
            appPrefsWrapper.getCardReaderPreferredPlugin(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId
            )
        ).thenReturn(null)
        whenever(dismissalStorage.isBannerHidden(any())).thenReturn(true)

        val result = sut.shouldShow()

        assertThat(result).isFalse()
    }

    @Test
    fun `given all conditions met for US, when shouldShow called, then returns true`() {
        val site = setupValidSite()
        whenever(wooPosIsScreenSizeAllowed()).thenReturn(false)
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("US")
        whenever(
            appPrefsWrapper.getCardReaderPreferredPlugin(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId
            )
        ).thenReturn(null)
        whenever(dismissalStorage.isBannerHidden(any())).thenReturn(false)

        val result = sut.shouldShow()

        assertThat(result).isTrue()
    }

    @Test
    fun `given all conditions met for GB, when shouldShow called, then returns true`() {
        val site = setupValidSite()
        whenever(wooPosIsScreenSizeAllowed()).thenReturn(false)
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("GB")
        whenever(
            appPrefsWrapper.getCardReaderPreferredPlugin(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId
            )
        ).thenReturn(null)
        whenever(dismissalStorage.isBannerHidden(any())).thenReturn(false)

        val result = sut.shouldShow()

        assertThat(result).isTrue()
    }

    @Test
    fun `when toJitmResponse called, then returns valid response`() {
        val response = sut.toJitmResponse()

        assertThat(response.id).isEqualTo("woo_pos_client_banner")
        assertThat(response.featureClass).isEqualTo("woo_pos_promotion")
        assertThat(response.cta.link).isEqualTo("https://woocommerce.com/in-person-payments/")
    }

    @Test
    fun `when messagePath accessed, then returns MY_STORE path`() {
        assertThat(sut.messagePath).isEqualTo(JitmMessagePathsProvider.MY_STORE)
    }

    @Test
    fun `when bannerId accessed, then returns correct id`() {
        assertThat(sut.bannerId).isEqualTo("woo_pos_client_banner")
    }

    @Test
    fun `when onDismiss called, then banner is hidden in storage`() {
        sut.onDismiss()

        verify(dismissalStorage).hideBanner(eq("woo_pos_client_banner"))
    }

    @Test
    fun `when onCtaClick called, then banner is hidden in storage`() {
        sut.onCtaClick()

        verify(dismissalStorage).hideBanner(eq("woo_pos_client_banner"))
    }

    private fun setupValidSite(): SiteModel {
        val site = SiteModel().apply {
            id = 1
            siteId = 99L
            selfHostedSiteId = 0L
        }
        whenever(selectedSite.getIfExists()).thenReturn(site)
        return site
    }
}
