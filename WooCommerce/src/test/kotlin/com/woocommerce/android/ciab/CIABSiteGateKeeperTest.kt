package com.woocommerce.android.ciab

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.given
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class CIABSiteGateKeeperTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val ciabSiteGateKeeper = CIABSiteGateKeeper(selectedSite)

    @Test
    fun `given current site is CIAB, when checking feature support, then feature is unsupported`() {
        val site = createSite(isCIAB = true)
        given(selectedSite.getOrNull()).willReturn(site)

        CIABAffectedFeature.entries.forEach {
            assertFalse(ciabSiteGateKeeper.isFeatureSupported(it))
        }
    }

    @Test
    fun `given current site is not CIAB, when checking feature support, then feature is supported`() {
        val site = createSite(isCIAB = false)
        given(selectedSite.getOrNull()).willReturn(site)

        CIABAffectedFeature.entries.forEach {
            assertTrue(ciabSiteGateKeeper.isFeatureSupported(it))
        }
    }

    private fun createSite(isCIAB: Boolean): SiteModel {
        return SiteModel().apply {
            setIsGardenSite(isCIAB)
            this.gardenName = if (isCIAB) CIABSiteGateKeeper.CIAB_GARDEN_NAME else null
        }
    }
}
