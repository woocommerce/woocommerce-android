package com.woocommerce.android.ui.moremenu.domain

import com.woocommerce.android.ciab.CIABAffectedFeature
import com.woocommerce.android.ciab.CIABSiteGateKeeper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class MoreMenuRepositoryTest : BaseUnitTest() {

    lateinit var sut: MoreMenuRepository

    val selectedSite: SelectedSite = mock()
    val getWooVersion: GetWooCorePluginCachedVersion = mock()
    val ciabSiteGateKeeper: CIABSiteGateKeeper = mock {
        on { isFeatureUnsupported(any()) } doReturn false
    }

    @Before
    fun setUp() {
        sut = MoreMenuRepository(
            selectedSite,
            getWooVersion,
            ciabSiteGateKeeper,
        )
    }

    @Test
    fun `show upgrades button when store is WPCOM`() {
        // given
        selectedSite.stub {
            on { getIfExists() } doReturn SiteModel().apply { setIsWpComStore(true) }
        }

        // when
        val isUpgradesEnabled = sut.isUpgradesEnabled()

        // then
        assertThat(isUpgradesEnabled).isTrue
    }

    @Test
    fun `hide upgrades button when store is not WPCOM`() {
        // given
        selectedSite.stub {
            on { getIfExists() } doReturn SiteModel().apply { setIsWpComStore(false) }
        }

        // when
        val isUpgradesEnabled = sut.isUpgradesEnabled()

        // then
        assertThat(isUpgradesEnabled).isFalse
    }

    @Test
    fun `hide upgrades button when store is not selected`() {
        // given
        selectedSite.stub {
            on { getIfExists() } doReturn null
        }

        // when
        val isUpgradesEnabled = sut.isUpgradesEnabled()

        // then
        assertThat(isUpgradesEnabled).isFalse
    }

    @Test
    fun `given Inbox feature is unsupported, when checking inbox, then inbox is disabled`() = testBlocking {
        // GIVEN
        whenever(selectedSite.exists()).thenReturn(true)
        whenever(ciabSiteGateKeeper.isFeatureUnsupported(CIABAffectedFeature.Inbox)).thenReturn(true)

        // WHEN
        val isInboxEnabled = sut.isInboxEnabled()

        // THEN
        assertThat(isInboxEnabled).isFalse
    }
}
