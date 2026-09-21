package org.wordpress.android.fluxc.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SiteModelTest {
    @Test
    fun `given otherwise equal sites, when HTTPS configuration differs, then equality and hash code differ`() {
        val unknown = SiteModel().apply {
            id = 1
            url = "https://test.com"
        }
        val requiresConfiguration = SiteModel().apply {
            id = 1
            url = "https://test.com"
            httpsConfigurationState = SiteModel.HTTPS_CONFIGURATION_REQUIRES_HTTPS
        }

        assertThat(unknown).isNotEqualTo(requiresConfiguration)
        assertThat(unknown.hashCode()).isNotEqualTo(requiresConfiguration.hashCode())
    }
}
