package com.woocommerce.android.ui.blaze

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IsBlazeEnabledTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()

    val sut = IsBlazeEnabled(selectedSite)

    @Test
    fun `given site without admin capabilities, when checking if Blaze is enabled, then Blaze is disabled`() {
        val site = createSite(isAdmin = false)
        given(selectedSite.getOrNull()).willReturn(site)

        val result = sut.invoke()

        assertThat(result).isFalse()
    }

    @Test
    fun `given site without Jetpack connection, when checking if Blaze is enabled, then Blaze is disabled`() {
        val site = createSite(isJetpackConnected = false)
        given(selectedSite.getOrNull()).willReturn(site)

        val result = sut.invoke()

        assertThat(result).isFalse()
    }

    @Test
    fun `given site without Blaze capability, when checking if Blaze is enabled, then Blaze is disabled`() {
        val site = createSite(canBlaze = false)
        given(selectedSite.getOrNull()).willReturn(site)

        val result = sut.invoke()

        assertThat(result).isFalse()
    }

    @Test
    fun `given site with Blaze for WooCommerce plugin active, when checking if Blaze is enabled, then Blaze is enabled`() {
        val site = createSite(
            isJetpackConnected = false,
            jetpackConnectionActivePlugins = IsBlazeEnabled.BLAZE_FOR_WOOCOMMERCE_PLUGIN_SLUG
        )
        given(selectedSite.getOrNull()).willReturn(site)

        val result = sut.invoke()

        assertThat(result).isTrue()
    }

    private fun createSite(
        isAdmin: Boolean = true,
        canBlaze: Boolean = true,
        isJetpackConnected: Boolean = true,
        jetpackConnectionActivePlugins: String? = null,
    ): SiteModel {
        return SiteModel().apply {
            this.hasCapabilityManageOptions = isAdmin
            this.canBlaze = canBlaze

            if (isJetpackConnected) {
                origin = SiteModel.ORIGIN_WPCOM_REST
                setIsJetpackConnected(isJetpackConnected)
            }

            activeJetpackConnectionPlugins = jetpackConnectionActivePlugins
        }
    }
}
