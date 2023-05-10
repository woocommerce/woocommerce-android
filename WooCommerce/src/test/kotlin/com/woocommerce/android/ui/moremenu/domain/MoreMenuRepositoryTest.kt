package com.woocommerce.android.ui.moremenu.domain

import com.woocommerce.android.tools.SelectedSite
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.wordpress.android.fluxc.model.SiteModel

class MoreMenuRepositoryTest {

    lateinit var sut: MoreMenuRepository

    val selectedSite: SelectedSite = mock()

    @Before
    fun setUp() {
        sut = MoreMenuRepository(
            selectedSite,
            mock(),
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
}
