package com.woocommerce.android.ciab

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.given
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class CIABSiteGateKeeperTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val ciabSiteGateKeeper = CIABSiteGateKeeper(selectedSite)

    @Test
    fun `given current site is CIAB, when checking restricted feature support, then feature is unsupported`() {
        // GIVEN
        val site = createSite(isCIAB = true)
        given(selectedSite.getOrNull()).willReturn(site)

        // WHEN / THEN
        CIABAffectedFeature.entries
            .filter {
                it != CIABAffectedFeature.POS &&
                    it != CIABAffectedFeature.InPersonPayments
            }
            .forEach {
                assertThat(ciabSiteGateKeeper.isFeatureSupported(it)).isFalse()
            }
    }

    @Test
    fun `given current site is CIAB without Pro plan, when checking InPersonPayments support, then feature is unsupported`() {
        // GIVEN
        val site = createSite(isCIAB = true, planSlug = "woo_hosted_free_plan")
        given(selectedSite.getOrNull()).willReturn(site)

        // WHEN
        val result = ciabSiteGateKeeper.isFeatureSupported(CIABAffectedFeature.InPersonPayments)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given CIAB site with Pro monthly plan, when checking IPP support, then feature is supported`() {
        // GIVEN
        val site = createSite(isCIAB = true, planSlug = "woo_hosted_pro_plan_monthly")
        given(selectedSite.getOrNull()).willReturn(site)

        // WHEN
        val result = ciabSiteGateKeeper.isFeatureSupported(CIABAffectedFeature.InPersonPayments)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given CIAB site with Pro yearly plan, when checking IPP support, then feature is supported`() {
        // GIVEN
        val site = createSite(isCIAB = true, planSlug = "woo_hosted_pro_plan_yearly")
        given(selectedSite.getOrNull()).willReturn(site)

        // WHEN
        val result = ciabSiteGateKeeper.isFeatureSupported(CIABAffectedFeature.InPersonPayments)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given CIAB site with free plan, when checking IPP support, then feature is unsupported`() {
        // GIVEN
        val site = createSite(isCIAB = true, planSlug = "woo_hosted_free_plan")
        given(selectedSite.getOrNull()).willReturn(site)

        // WHEN
        val result = ciabSiteGateKeeper.isFeatureSupported(CIABAffectedFeature.InPersonPayments)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given non-CIAB site, when checking IPP support, then feature is supported`() {
        // GIVEN
        val site = createSite(isCIAB = false)
        given(selectedSite.getOrNull()).willReturn(site)

        // WHEN
        val result = ciabSiteGateKeeper.isFeatureSupported(CIABAffectedFeature.InPersonPayments)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `when checking POS support, then feature is always supported`() {
        // WHEN / THEN
        assertThat(ciabSiteGateKeeper.isFeatureSupported(CIABAffectedFeature.POS)).isTrue()
    }

    @Test
    fun `given current site is not CIAB, when checking feature support, then feature is supported`() {
        // GIVEN
        val site = createSite(isCIAB = false)
        given(selectedSite.getOrNull()).willReturn(site)

        // WHEN / THEN
        CIABAffectedFeature.entries
            .forEach {
                assertThat(ciabSiteGateKeeper.isFeatureSupported(it)).isTrue()
            }
    }

    private fun createSite(isCIAB: Boolean, planSlug: String? = null): SiteModel {
        return SiteModel().apply {
            setIsGardenSite(isCIAB)
            this.gardenName = if (isCIAB) SiteModel.CIAB_GARDEN_NAME else null
            this.planProductSlug = planSlug
        }
    }
}
