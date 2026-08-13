package com.woocommerce.android.tools

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel

class SiteConnectionTypeTest {
    @Test
    fun `given a site not from the WPCom REST origin, when the type is read, then it is application passwords`() {
        val site = SiteModel().apply { origin = SiteModel.ORIGIN_WPAPI }

        assertThat(site.connectionTypeOrNull).isEqualTo(SiteConnectionType.ApplicationPasswords)
    }

    @Test
    fun `given a Jetpack connected site, when the type is read, then it is Jetpack`() {
        val site = SiteModel().apply {
            origin = SiteModel.ORIGIN_WPCOM_REST
            setIsJetpackConnected(true)
        }

        assertThat(site.connectionTypeOrNull).isEqualTo(SiteConnectionType.Jetpack)
    }

    @Test
    fun `given a Jetpack CP connected site, when the type is read, then it is the connection package`() {
        val site = SiteModel().apply {
            origin = SiteModel.ORIGIN_WPCOM_REST
            setIsJetpackCPConnected(true)
        }

        assertThat(site.connectionTypeOrNull).isEqualTo(SiteConnectionType.JetpackConnectionPackage)
    }

    /**
     * The case `connectionType` cannot express: it has to return a value, so in production it answers `Jetpack`
     * here, which a caller reporting the value would pass off as a real Jetpack connection.
     */
    @Test
    fun `given a site matching no known case, when the nullable type is read, then it is null`() {
        val site = SiteModel().apply {
            origin = SiteModel.ORIGIN_WPCOM_REST
            setIsJetpackConnected(false)
            setIsJetpackCPConnected(false)
        }

        assertThat(site.connectionTypeOrNull).isNull()
    }
}
