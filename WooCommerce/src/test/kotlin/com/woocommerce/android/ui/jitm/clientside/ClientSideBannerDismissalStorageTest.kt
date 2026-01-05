package com.woocommerce.android.ui.jitm.clientside

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class ClientSideBannerDismissalStorageTest : BaseUnitTest() {

    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val selectedSite: SelectedSite = mock()

    private lateinit var sut: ClientSideBannerDismissalStorage

    @Before
    fun setup() {
        sut = ClientSideBannerDismissalStorage(
            appPrefsWrapper = appPrefsWrapper,
            selectedSite = selectedSite,
        )
    }

    @Test
    fun `given no site selected, when isBannerHidden called, then returns false`() {
        whenever(selectedSite.getIfExists()).thenReturn(null)

        val result = sut.isBannerHidden("banner_id")

        assertThat(result).isFalse()
    }

    @Test
    fun `given site selected and banner hidden, when isBannerHidden called, then returns true`() {
        val site = createSite()
        whenever(selectedSite.getIfExists()).thenReturn(site)
        whenever(
            appPrefsWrapper.isClientSideBannerHidden(
                bannerId = "banner_id",
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId
            )
        ).thenReturn(true)

        val result = sut.isBannerHidden("banner_id")

        assertThat(result).isTrue()
    }

    @Test
    fun `given site selected and banner not hidden, when isBannerHidden called, then returns false`() {
        val site = createSite()
        whenever(selectedSite.getIfExists()).thenReturn(site)
        whenever(
            appPrefsWrapper.isClientSideBannerHidden(
                bannerId = "banner_id",
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId
            )
        ).thenReturn(false)

        val result = sut.isBannerHidden("banner_id")

        assertThat(result).isFalse()
    }

    @Test
    fun `given no site selected, when hideBanner called, then prefs not updated`() {
        whenever(selectedSite.getIfExists()).thenReturn(null)

        sut.hideBanner("banner_id")

        verify(appPrefsWrapper, never()).setClientSideBannerHidden(
            bannerId = eq("banner_id"),
            isHidden = eq(true),
            localSiteId = eq(0),
            remoteSiteId = eq(0L),
            selfHostedSiteId = eq(0L)
        )
    }

    @Test
    fun `given site selected, when hideBanner called, then prefs updated correctly`() {
        val site = createSite()
        whenever(selectedSite.getIfExists()).thenReturn(site)

        sut.hideBanner("banner_id")

        verify(appPrefsWrapper).setClientSideBannerHidden(
            bannerId = "banner_id",
            isHidden = true,
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId
        )
    }

    private fun createSite(): SiteModel {
        return SiteModel().apply {
            id = 1
            siteId = 123L
            selfHostedSiteId = 456L
        }
    }
}
